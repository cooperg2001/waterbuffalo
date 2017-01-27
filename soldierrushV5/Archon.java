package soldierrushV5;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Archon {

    static void runArchon(RobotController rc) throws GameActionException {
        int round_tree_was_planted_last = 0;
        int num_trees_last = 0;
        int numGardenersLast = 0;
        int roundLastGardener = 0;

		boolean hasBroadcasted = false;
		int numArchon = rc.readBroadcast(900);
		if(numArchon == 0){
			rc.broadcast(500, 999999);    // Archon
			rc.broadcast(501, 999999); // Gardener
			rc.broadcast(502, 999999); // Lumberjack
			rc.broadcast(503, 999999); // Scout
			rc.broadcast(504, 999999); // Soldier
			rc.broadcast(505, 999999); // Tank
		}
		rc.broadcast(900, rc.readBroadcast(900) + 1);

        Direction rand = RobotPlayer.randomDirection();

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

                int archon_ct = rc.readBroadcast(900);
                int gardener_ct = rc.readBroadcast(901);
                int lumberjack_ct = rc.readBroadcast(902);
                int scout_ct = rc.readBroadcast(903);
                int soldier_ct = rc.readBroadcast(904);
                int tank_ct = rc.readBroadcast(905);
                int army_size = scout_ct + soldier_ct + tank_ct;
                int tree_ct = rc.getTreeCount();
                int round_num = rc.getRoundNum();
                boolean didBuildGardener = false;

                RobotPlayer.updateEnemiesAndBroadcast();
				
				if(rc.readBroadcast(501) == 999999 && !hasBroadcasted){
					int encode = (int)RobotPlayer.their_archons[numArchon].x * 1000 + (int)RobotPlayer.their_archons[numArchon].y;
					rc.broadcast(500, encode);    // Archon
					rc.broadcast(501, encode); // Gardener
					hasBroadcasted = true;
				}

				if (rc.getTreeCount() > num_trees_last){
                    round_tree_was_planted_last = round_num;
                    num_trees_last = rc.getTreeCount();
                }

                if(gardener_ct > numGardenersLast){
				    roundLastGardener = round_num;
				    numGardenersLast = gardener_ct;
                }


                //Check for hiring gardeners if no more trees can be planted
                if(rc.readBroadcast(7) == 1 && rc.readBroadcast(8) < rc.getRoundNum()){
                    // Check all angles around us for potential build locations
                    for (int i = 0; i < RobotPlayer.num_angles; i++) {
                        Direction dir = RobotPlayer.absolute_right.rotateLeftRads(RobotPlayer.potential_angles[i]);
                        if (rc.canHireGardener(dir) && (roundLastGardener + 50 < round_num || gardener_ct == 0)) {
                            // We can hire a gardener, and we have a sufficiently big army to justify hiring gardeners
                            // Try to make sure hiring a gardener doesn't trap our archon
                            if (rc.getRoundNum() > 30) {
                                rc.hireGardener(dir);
                                rc.broadcast(901, gardener_ct + 1);
                                gardener_ct++;
                                didBuildGardener = true;
                            } else if (rc.canMove(dir.rotateLeftDegrees(90))) {
                                rc.hireGardener(dir);
                                rc.broadcast(901, gardener_ct + 1);
                                gardener_ct++;
                                didBuildGardener = true;
                                rc.move(dir.rotateLeftDegrees(90));
                            } else if (rc.canMove(dir.rotateRightDegrees(90))) {
                                rc.hireGardener(dir);
                                rc.broadcast(901, gardener_ct + 1);
                                gardener_ct++;
                                rc.move(dir.rotateRightDegrees(90));
                                didBuildGardener = true;
                            }
                            round_tree_was_planted_last = round_num;
                            break;
                        }
                    }
                }

                MapLocation target = RobotPlayer.get_best_location();
                if(!target.equals(RobotPlayer.INVALID_LOCATION) && rc.canMove(target) && !rc.hasMoved()){
                    rc.move(target);
                    rand = RobotPlayer.randomDirection();
                }
                else{
                    if(rc.canMove(rand) && !rc.hasMoved()){
                        rc.move(rand);
                    }
                    else{
                        int trials = 0;
                        while(!rc.canMove(rand) && trials < 10){
                            rand = RobotPlayer.randomDirection();
                            trials++;
                        }
                        if(rc.canMove(rand) && !rc.hasMoved()){
                            rc.move(rand);
                        }
                    }
                }
                if(didBuildGardener){
                    rc.broadcast(7, 0); //we took care of it, wait up for next trigger
                    rc.broadcast(8, rc.getRoundNum());
                } else if(rc.readBroadcast(7) == 0){
                    rc.broadcast(7, 1); //reset this broadcast to "should hire gardener" for the next round
                    rc.broadcast(8,rc.getRoundNum());
                }


                Clock.yield();
            } catch(Exception e){
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
}
