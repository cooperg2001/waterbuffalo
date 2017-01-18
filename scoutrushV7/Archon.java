package scoutrushV7;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Archon {

    static void runArchon(RobotController rc) throws GameActionException {

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

                RobotPlayer.updateEnemiesAndBroadcast();
				
				if(rc.readBroadcast(501) == 999999 && !hasBroadcasted){
					int encode = (int)RobotPlayer.their_archons[numArchon].x * 1000 + (int)RobotPlayer.their_archons[numArchon].y;
					rc.broadcast(500, encode);    // Archon
					rc.broadcast(501, encode); // Gardener
					hasBroadcasted = true;
				}

                // Check all angles around us for potential build locations
                for(int i = 0; i < RobotPlayer.num_angles; i++){
                    Direction dir = RobotPlayer.absolute_right.rotateLeftRads(RobotPlayer.potential_angles[i]);
                    if(rc.canHireGardener(dir) && ((rc.readBroadcast(904) + rc.readBroadcast(903) + 1) > 5 * rc.readBroadcast(901) || rc.getTeamBullets() > 310)){
                        // We can hire a gardener, and we have a sufficiently big army to justify hiring gardeners
                        // Try to make sure hiring a gardener doesn't trap our archon
                        if(rc.getRoundNum() > 30){
                            rc.hireGardener(dir);
                            rc.broadcast(901, rc.readBroadcast(901) + 1);
                        }
                        else if(rc.canMove(dir.rotateLeftDegrees(90))){
                            rc.hireGardener(dir);
                            rc.broadcast(901, rc.readBroadcast(901) + 1);
                            rc.move(dir.rotateLeftDegrees(90));
                        }
                        else if(rc.canMove(dir.rotateRightDegrees(90))){
                            rc.hireGardener(dir);
                            rc.broadcast(901, rc.readBroadcast(901) + 1);
                            rc.move(dir.rotateRightDegrees(90));
                        }
                        break;
                    }
                }

                MapLocation target = RobotPlayer.get_best_location();
                if(target.x != RobotPlayer.INVALID_LOCATION.x && rc.canMove(target) && !rc.hasMoved()){
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

                Clock.yield();
            } catch(Exception e){
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
}
