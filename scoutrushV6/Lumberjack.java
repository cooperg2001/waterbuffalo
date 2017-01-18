package scoutrushV6;

import battlecode.common.*;

public class Lumberjack {

    static void runLumberjack(RobotController rc) throws GameActionException {

        Direction rand = RobotPlayer.randomDirection();
        boolean sentDeathSignal = false; // true if we've sent a death signal

        boolean hasTreeTarget = false; // true if currently chopping down a tree

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
                // If adjacent to enemies, strike
                for (RobotInfo robot : RobotPlayer.robots){
                    if (robot.getTeam() == RobotPlayer.ENEMY
                            && robot.getLocation().distanceTo(rc.getLocation()) - robot.getRadius() < 2
                            && rc.canStrike()){
                        rc.strike();
                    }
                }

                // Find closest neutral tree
                if (RobotPlayer.neutral_trees.length > 0) {
                    TreeInfo closest_tree = RobotPlayer.neutral_trees[0];
                    for (int i = 0; i < RobotPlayer.neutral_trees.length; i++) {
                        TreeInfo test_tree = RobotPlayer.neutral_trees[i];
                        if (RobotPlayer.getDistToTree(test_tree) < RobotPlayer.getDistToTree(closest_tree)) {
                            closest_tree = test_tree;
                        }
                    }
                    // Test to see if lumberjack can chop down closest neutral tree
                    if (rc.canChop(closest_tree.location)){
                        hasTreeTarget = true;
                        // If lumberjack is about to kill tree, set target to false
                        if(closest_tree.getHealth() <= 5){
                            hasTreeTarget = false;
                        }
                        rc.chop(closest_tree.location);
                    }
                }

                /**
                 * If lumberjack isn't cutting down a tree,
                 * move in direction lumberjack was previously moving,
                 * else move randomly
                 */
                if (!hasTreeTarget){
                    int trials = 0;
                    while(!rc.canMove(rand) && trials < 10){
                        rand = RobotPlayer.randomDirection();
                        trials++;
                    }
                    if(rc.canMove(rand)) {
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
