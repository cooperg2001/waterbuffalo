package soldierrushV4;

import battlecode.common.*;

import java.util.TreeMap;

public class Lumberjack {

    static void runLumberjack(RobotController rc) throws GameActionException {
        MapLocation spawn = rc.getLocation();
        Direction rand = RobotPlayer.randomDirection();
        boolean sentDeathSignal = false; // true if we've sent a death signal

        /**
         * GENERAL BEHAVIOR
         *
         * (1) Unless disturbed, stay as close to spawn as possible and chop down nearby trees
         * --- Heuristic for choosing trees is seeing which trees are closest to spawn. Does not utilize broadcast.
         *
         * (2) If it senses an enemy unit, it chases the enemy unit away from Lumberjack's spawn.
         * --- If lumberjack gets stuck but can still see enemy unit, it attempts to cut any trees in the way
         *
         * (3) If it gets too far away from its own spawn, Lumberjack returns to it.
         * --- Currently retreats if roundNum() < 15 * distance from spawn.
         * --- Implication: Does not fully explore any map before 1500 rounds.
         * --- Ceases retreat once roundNum() < 7.5 * distance from spawn.
         */

        /**
         * STATUS INDICES
         * 0 - Chopping trees
         * 1 - Chasing enemies
         * 2 - Retreating
         */

        int status = 0;

        while(true){
            try{
                RobotPlayer.findShakableTrees();
                RobotPlayer.checkForStockpile();

                RobotPlayer.robots = rc.senseNearbyRobots();
                RobotPlayer.trees = rc.senseNearbyTrees(-1);
                RobotPlayer.neutral_trees = rc.senseNearbyTrees(-1, RobotPlayer.NEUTRAL);
                RobotPlayer.their_trees = rc.senseNearbyTrees(-1, RobotPlayer.ENEMY);
                RobotPlayer.our_trees = rc.senseNearbyTrees(-1, RobotPlayer.FRIEND);
                RobotPlayer.bullets = rc.senseNearbyBullets();

                RobotPlayer.updateEnemiesAndBroadcast();

                MapLocation current_location = rc.getLocation();
                RobotInfo[] their_robots = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);
                float distance_from_spawn = current_location.distanceTo(spawn);

                // If more than roundnum/15 away from spawn point, set to retreat
                if (0 * distance_from_spawn > rc.getRoundNum()){
                    status = 2;
                }
                // Else if can see enemies, set to attack
                else if (their_robots.length > 0){
                    status = 1;
                }


                // RETREAT MODE
                if (status == 2){

                    // Move towards spawn
                    Direction toSpawn = current_location.directionTo(spawn);
                    if (rc.canMove(toSpawn)){
                        rc.move(toSpawn);
                    }

                    // If can't, chop down a tree if it's in the way
                    else{
                        if (RobotPlayer.neutral_trees.length > 0){
                            TreeInfo closest_tree = RobotPlayer.neutral_trees[0];
                            if (rc.canChop(closest_tree.location)){
                                rc.chop(closest_tree.location);
                            }
                        }
                        // If can't, move randomly
                        else{
                            int trials = 0;
                            while (!rc.canMove(rand) && trials < 10) {
                                rand = RobotPlayer.randomDirection();
                                trials += 1;
                            }
                            if (rc.canMove(rand)) {
                                rc.move(rand);
                            }
                        }
                    }

                    // Test status
                    if (distance_from_spawn < 7.5 * rc.getRoundNum()){
                        status = 0;
                    }
                }

                // CHASE MODE
                else if (status == 1){
                    // Move towards closest enemy
                    if (their_robots.length > 0){
                        for (RobotInfo enemy : their_robots) {
                            if (rc.canMove(current_location.directionTo(enemy.location), current_location.distanceTo(enemy.location))
                                    && !rc.hasMoved()) {
                                rc.move(current_location.directionTo(enemy.location), current_location.distanceTo(enemy.location));
                            }
                        }

                        // If adjacent to an enemy, strike
                        RobotInfo enemy = their_robots[0];
                        if (enemy.getLocation().distanceTo(current_location) - enemy.getRadius() < 2
                                && rc.canStrike()){
                            rc.strike();
                        }

                        // If haven't done anything yet, it's blocked by stuff. See if it can chop down a tree.
                        if (!rc.hasMoved() && !rc.hasAttacked()) {
                            if (RobotPlayer.neutral_trees.length > 0){
                                TreeInfo closest_tree = RobotPlayer.neutral_trees[0];
                                if (rc.canChop(closest_tree.location)){
                                    rc.chop(closest_tree.location);
                                }
                            }
                        }

                    }

                    // If this list is actually empty, set status to 2
                    else {
                        status = 0;
                    }
                }

                // CHOP TREES MODE
                else if (status == 0) {
                    // If you can't see any neutral trees, move randomly
                    if (RobotPlayer.neutral_trees.length == 0 && RobotPlayer.their_trees.length == 0) {
                        int trials = 0;
                        while (!rc.canMove(rand) && trials < 10) {
                            rand = RobotPlayer.randomDirection();
                            trials += 1;
                        }
                        if (rc.canMove(rand)) {
                            rc.move(rand);
                        }
                    }

                    // Else, start chopping!
                    else {
                        // If it can chop a tree, chop it down.
                        TreeInfo closest_tree;
                        if (RobotPlayer.neutral_trees.length > 0) {
                            closest_tree = RobotPlayer.neutral_trees[0];
                        } else if (RobotPlayer.their_trees.length > 0) {
                            closest_tree = RobotPlayer.their_trees[0];
                        } else {
                            closest_tree = null;
                        }
                        if (closest_tree != null && rc.canChop(closest_tree.location)) {
                            rc.chop(closest_tree.location);
                        }
                        // Else, can't chop down trees. Move towards tree and chop it.
                        else {
                            // If we can see a friendly archon, move towards the tree closest to it and chop it down.
                            for (RobotInfo robot : RobotPlayer.robots) {
                                if (robot.getType() == RobotType.ARCHON && robot.getTeam() == RobotPlayer.FRIEND) {
                                    RobotInfo archon = robot;
                                    TreeMap<Float, TreeInfo> trees_by_archon_dist = new TreeMap<Float, TreeInfo>();
                                    for (TreeInfo tree : RobotPlayer.neutral_trees) {
                                        trees_by_archon_dist.put(tree.location.distanceTo(archon.getLocation()), tree);
                                    }
                                    for (TreeInfo tree : RobotPlayer.their_trees) {
                                        trees_by_archon_dist.put(tree.location.distanceTo(archon.getLocation()), tree);
                                    }
                                    outerloop:
                                    //used to break out of nested FOR loops
                                    for (float tree_archon_dist : trees_by_archon_dist.keySet()) {
                                        TreeInfo tree = trees_by_archon_dist.get(tree_archon_dist);
                                        Direction target_to_robot = tree.getLocation().directionTo(rc.getLocation());
                                        for (int i = 0; i < RobotPlayer.num_angles; i++) {
                                            Direction target_dir;
                                            if (i % 2 == 0) {
                                                target_dir = target_to_robot.rotateLeftRads((int) (i + 1) / 2 * 2 * (float) Math.PI / RobotPlayer.num_angles);
                                            } else {
                                                target_dir = target_to_robot.rotateRightRads((int) (i + 1) / 2 * 2 * (float) Math.PI / RobotPlayer.num_angles);
                                            }
                                            MapLocation target_loc = closest_tree.getLocation().add(target_dir, closest_tree.getRadius() + rc.getType().bodyRadius);
                                            if (rc.canMove(target_loc)) {
                                                rc.move(target_loc);
                                                break outerloop;
                                            }
                                        }
                                        // Chop if you can
                                        if (rc.canChop(tree.location)) {
                                            rc.chop(tree.location);
                                        }
                                    }

                                }
                                // If we can't see a friendly archon, go by own spawn point.
                                TreeMap<Float, TreeInfo> trees_by_spawn_dist = new TreeMap<Float, TreeInfo>();
                                for (TreeInfo tree : RobotPlayer.neutral_trees) {
                                    trees_by_spawn_dist.put(tree.location.distanceTo(spawn), tree);
                                }
                                for (TreeInfo tree : RobotPlayer.their_trees) {
                                    trees_by_spawn_dist.put(tree.location.distanceTo(spawn), tree);
                                }
                                outerloop:
                                //used to break out of nested FOR loops
                                for (float tree_spawn_dist : trees_by_spawn_dist.keySet()) {
                                    TreeInfo tree = trees_by_spawn_dist.get(tree_spawn_dist);
                                    Direction target_to_robot = tree.getLocation().directionTo(rc.getLocation());
                                    for (int i = 0; i < RobotPlayer.num_angles; i++) {
                                        Direction target_dir;
                                        if (i % 2 == 0) {
                                            target_dir = target_to_robot.rotateLeftRads((int) (i + 1) / 2 * 2 * (float) Math.PI / RobotPlayer.num_angles);
                                        } else {
                                            target_dir = target_to_robot.rotateRightRads((int) (i + 1) / 2 * 2 * (float) Math.PI / RobotPlayer.num_angles);
                                        }
                                        MapLocation target_loc = closest_tree.getLocation().add(target_dir, closest_tree.getRadius() + rc.getType().bodyRadius);
                                        if (rc.canMove(target_loc)) {
                                            rc.move(target_loc);
                                            break outerloop;
                                        }
                                    }
                                    // Chop if you can
                                    if (rc.canChop(tree.location)) {
                                        rc.chop(tree.location);
                                    }
                                }

                            }
                            if (!rc.hasMoved() && !rc.hasAttacked()) {
                                int trials = 0;
                                while (!rc.canMove(rand) && trials < 10) {
                                    rand = RobotPlayer.randomDirection();
                                    trials += 1;
                                }
                                if (rc.canMove(rand)) {
                                    rc.move(rand);
                                }
                            }
                        }
                    }
                }

                else{
                    System.out.println("Invalid status");
                }

                // Death signal protocol
                if(!sentDeathSignal && rc.getHealth() < 8) {
                    sentDeathSignal = true;
                    rc.broadcast(900 + RobotPlayer.typeToInt(rc.getType()), rc.readBroadcast(900 + RobotPlayer.typeToInt(rc.getType())) - 1);
                }

                Clock.yield();
            } catch(Exception e){
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
}
