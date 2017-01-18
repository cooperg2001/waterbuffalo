package scoutrushV7;

import battlecode.common.*;

public class Lumberjack {

    static void runLumberjack(RobotController rc) throws GameActionException {
        MapLocation spawn = rc.getLocation();
        boolean hasTree = false;
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

                RobotPlayer.updateEnemiesAndBroadcast();
                // If adjacent to enemies, strike
                for (RobotInfo robot : RobotPlayer.robots){
                    if (robot.getTeam() == RobotPlayer.ENEMY
                            && robot.getLocation().distanceTo(rc.getLocation()) - robot.getRadius() < 2
                            && rc.canStrike()){
                        rc.strike();
                    }
                }

                // Find neutral tree closest to spawn, move to it if possible
                if (RobotPlayer.neutral_trees.length > 0) {
                    TreeInfo closest_tree = RobotPlayer.neutral_trees[0];
                    for (int i = 0; i < RobotPlayer.neutral_trees.length; i++) {
                        TreeInfo test_tree = RobotPlayer.neutral_trees[i];
                        if (spawn.distanceTo(test_tree.getLocation()) < spawn.distanceTo(closest_tree.getLocation())) {
                            closest_tree = test_tree;
                        }
                    }
					Direction target_to_robot = closest_tree.getLocation().directionTo(rc.getLocation());
					for(int i = 0; i < RobotPlayer.num_angles; i++){
						Direction target_dir;
						if(i % 2 == 0){
							target_dir = target_to_robot.rotateLeftRads((int)(i + 1)/2 * 2 * (float)Math.PI / RobotPlayer.num_angles);
						}
						else{
							target_dir = target_to_robot.rotateRightRads((int)(i + 1)/2 * 2 * (float)Math.PI / RobotPlayer.num_angles);
						}
						MapLocation target_loc = closest_tree.getLocation().add(target_dir, 2);
						if(rc.canMove(target_loc)){
							rc.move(target_loc);
						}
					}
                    // Test to see if lumberjack can chop down closest neutral tree
                    if (rc.canChop(closest_tree.location)){
                        hasTree = true;
                        if (closest_tree.getHealth() < 6){
                            hasTree = false;
                        }
                        rc.chop(closest_tree.location);
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
