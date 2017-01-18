package scoutrushV6;

import battlecode.common.*;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.List;

public class Lumberjack {

    static void runLumberjack(RobotController rc) throws GameActionException {
        MapLocation spawn = rc.getLocation();
        boolean hasTree = false;
        TreeInfo target_tree;
        Direction rand = RobotPlayer.randomDirection();
        boolean sentDeathSignal = false; // true if we've sent a death signal

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

                // initialize target tree for convenience
                target_tree = RobotPlayer.trees[0];

                RobotPlayer.updateEnemiesAndBroadcast();
                // If adjacent to enemies, strike
                for (RobotInfo robot : RobotPlayer.robots){
                    if (robot.getTeam() == RobotPlayer.ENEMY
                            && robot.getLocation().distanceTo(rc.getLocation()) - robot.getRadius() < 2
                            && rc.canStrike()){
                        rc.strike();
                    }
                }

                // Test if really next to a tree
                List<TreeInfo> neutral_trees_as_list = Arrays.asList(RobotPlayer.neutral_trees);
                if (!neutral_trees_as_list.contains(target_tree) || !rc.canChop(target_tree.location)){
                    hasTree = false;
                    System.out.println("no tree :(");
                }

                // If adjacent to a tree, do not move unless provoked - cut tree

                for (TreeInfo tree : RobotPlayer.neutral_trees){
                    if (rc.canChop(tree.location)){
                        hasTree = true;
                        System.out.println("have tree :D");
                        target_tree = tree;
                        rc.chop(tree.location);
                    }
                }

                // If not chopping down a tree, find neutral tree closest to spawn, move to it if possible
                if (RobotPlayer.neutral_trees.length > 0 && !hasTree) {
                    TreeMap<Float, TreeInfo> seen_neutral_trees = new TreeMap<Float, TreeInfo>();
                    for (TreeInfo tree : RobotPlayer.neutral_trees) {
                        seen_neutral_trees.put(tree.location.distanceTo(spawn), tree);
                    }
                    for (float distance_to_spawn : seen_neutral_trees.keySet()) {
                        TreeInfo tree = seen_neutral_trees.get(distance_to_spawn);
                        if (rc.canMove(rc.getLocation().directionTo(tree.location), rc.getLocation().distanceTo(tree.location))
                                && !rc.hasMoved()) {
                            rc.move(rc.getLocation().directionTo(tree.location), rc.getLocation().distanceTo(tree.location));
                        }
                    }
                }
                // Chop down any adjacent tree
                for (TreeInfo tree : RobotPlayer.neutral_trees){
                    if (rc.canChop(tree.location)){
                        hasTree = true;
                        System.out.println("have tree :D");
                        target_tree = tree;
                        rc.chop(tree.location);
                    }
                }

                // If the bot can't see trees or hasn't moved yet, move randomly
                if (RobotPlayer.neutral_trees.length == 0
                        || (!hasTree && !rc.hasMoved())) {
                    int trials = 0;
                    while (!rc.canMove(rand) && trials < 10) {
                        rand = RobotPlayer.randomDirection();
                        trials += 1;
                    }
                    if (rc.canMove(rand)) {
                        rc.move(rand);
                    }
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
