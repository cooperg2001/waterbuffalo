package scoutrushV7;

import battlecode.common.*;
import java.util.List;
import java.util.Arrays;
import java.util.TreeMap;

public class Lumberjack {

    static void runLumberjack(RobotController rc) throws GameActionException {
        MapLocation spawn = rc.getLocation();
        Direction rand = RobotPlayer.randomDirection();
        boolean sentDeathSignal = false; // true if we've sent a death signal

        /**
         * STATUS INDICES
         * 0 - Chopping trees
         * 1 - Chasing enemies
         * 2 - Retreating
         */

        boolean hasTree = false;

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
                TreeInfo target_tree = RobotPlayer.trees[0];

                // If more than 30 away from spawn point, set to retreat
                if (distance_from_spawn > 30){
                    status = 2;
                }
                // Else if can see enemies, set to attack
                else if (their_robots.length > 0){
                    status = 1;
                }


                // RETREAT MODE
                if (status == 2){
                    hasTree = false;

                    // Move towards spawn
                    Direction toSpawn = current_location.directionTo(spawn);
                    if (rc.canMove(toSpawn)){
                        rc.move(toSpawn);
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

                    // Test status
                    if (distance_from_spawn < 5){
                        status = 0;
                    }
                }

                // CHASE MODE
                else if (status == 1){
                    hasTree = false;
                    // Move towards closest enemy
                    if (their_robots.length > 0){
                        for (RobotInfo enemy : their_robots) {
                            if (rc.canMove(current_location.directionTo(enemy.location), current_location.distanceTo(enemy.location))
                                    && !rc.hasMoved()) {
                                rc.move(current_location.directionTo(enemy.location), current_location.distanceTo(enemy.location));
                            }
                        }

                        // If haven't moved yet, move randomly
                        if (!rc.hasMoved()) {
                            int trials = 0;
                            while (!rc.canMove(rand) && trials < 10) {
                                rand = RobotPlayer.randomDirection();
                                trials += 1;
                            }
                            if (rc.canMove(rand)) {
                                rc.move(rand);
                            }
                        }
                        // If adjacent to an enemy, strike
                        RobotInfo enemy = their_robots[0];
                        if (enemy.getLocation().distanceTo(current_location) - enemy.getRadius() < 2
                                && rc.canStrike()){
                            rc.strike();
                        }
                    }

                    // If this list is actually empty, set status to 2
                    else {
                        status = 0;
                    }
                }

                // CHOP TREES MODE
                else if (status == 0){
                    // If you can't see any neutral trees, move randomly
                    if (RobotPlayer.neutral_trees.length == 0) {
                        System.out.println("Where dem trees at");
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
                    else{
                        // If it's got a tree, keep chopping
                        if (hasTree){
                            // First check to see if it really has a tree
                            List<TreeInfo> trees_as_list = Arrays.asList(RobotPlayer.neutral_trees);
                            if (!trees_as_list.contains(target_tree) || !rc.canChop(target_tree.location)){
                                hasTree = false;
                            }
                            else {
                                System.out.println("I got me a tree");
                                if (rc.canChop(target_tree.location)) {
                                    rc.chop(target_tree.location);
                                }
                            }
                        }

                        // Otherwise, find a tree to chop
                        else{
                            // Move towards tree if possible, then chop
                            TreeMap<Float, TreeInfo> trees_by_spawn_dist = new TreeMap<Float, TreeInfo>();
                            for (TreeInfo tree : RobotPlayer.neutral_trees) {
                                trees_by_spawn_dist.put(tree.location.distanceTo(spawn), tree);
                            }
                            for (float tree_spawn_dist : trees_by_spawn_dist.keySet()){
                                TreeInfo tree = trees_by_spawn_dist.get(tree_spawn_dist);
                                // Move towards tree if possible
                                if (!hasTree && rc.canMove(current_location.directionTo(tree.location), current_location.distanceTo(tree.location))) {
                                    rc.move(current_location.directionTo(tree.location), current_location.distanceTo(tree.location));
                                }
                                // Chop if you can
                                if (rc.canChop(tree.location)){
                                    target_tree = tree;
                                    hasTree = true;
                                    rc.chop(target_tree.location);
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
