package scoutrushV1;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Scout {

    static void runScout(RobotController rc) throws GameActionException {
        rc.broadcast(904, rc.readBroadcast(904) + 1);
        Direction rand = RobotPlayer.randomDirection();
        int target_id = -1;
        MapLocation target_loc = RobotPlayer.invalid_location;

        while(true) {

            try{
                //System.out.println("Before updating surroundings: " + Clock.getBytecodeNum());
                //updateSurroundings();
                //System.out.println("After updating surroundings: " + Clock.getBytecodeNum());
                RobotPlayer.findShakableTrees();

                //System.out.println("Made it A");

                RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);

                boolean targetAlive = false;
                for(int i = 0; i < enemies.length; i++){
                    if(enemies[i].getID() == target_id){
                        targetAlive = true;
                        target_loc = enemies[i].getLocation();
                    }
                }

                //System.out.println("Made it B");

                if(!targetAlive){
                    target_id = -1;
                    target_loc = RobotPlayer.invalid_location;
                }

                if(target_id == -1){
                    if(enemies.length > 0){
                        int priority = -1;
                        for(int i = 0; i < enemies.length; i++){
                            if(priority == -1){
                                if((enemies[i].getType() == RobotType.SCOUT || enemies[i].getType() == RobotType.ARCHON) && rc.getTeamBullets() < 200){
                                    priority = -1;
                                }
                                else{
                                    priority = i;
                                }
                            }
                            else if(enemies[priority].getType() != RobotType.GARDENER && enemies[i].getType() == RobotType.GARDENER){
                                priority = i;
                            }
                            else if(enemies[priority].getType() != RobotType.GARDENER && enemies[priority].getType() != RobotType.SOLDIER && enemies[i].getType() == RobotType.SOLDIER){
                                priority = i;
                            }
                            else if(enemies[priority].getType() != RobotType.GARDENER && enemies[priority].getType() != RobotType.SOLDIER && enemies[priority].getType() != RobotType.LUMBERJACK && enemies[i].getType() == RobotType.LUMBERJACK){
                                priority = i;
                            }
                        }
                        if(priority != -1){
                            target_id = enemies[priority].getID();
                            target_loc = enemies[priority].getLocation();
                        }
                    }
                }

                //System.out.println("Made it C");

                MapLocation target;

                if(RobotPlayer.isValidLoc(target_loc)){
                    target = RobotPlayer.getBestShootingLocation(target_loc);
                }
                else{
                    target = RobotPlayer.invalid_location;
                }

                //System.out.println("Made it D");

                if(RobotPlayer.isValidLoc(target)){
                    //System.out.println("Target acquired... " + Clock.getBytecodeNum());
                    if(rc.canMove(target)){
                        rc.move(target);
                        if(rc.canFireSingleShot()){
                            rc.fireSingleShot(rc.getLocation().directionTo(target_loc));
                        }
                    }
                    else{
                        int trials = 0;
                        while(!rc.canMove(rand) && trials < 10){
                            rand = RobotPlayer.randomDirection();
                            trials++;
                        }
                        if(rc.canMove(rand)){
                            rc.move(rand);
                        }
                    }
                }
                else{
                    if(!RobotPlayer.isValidLoc(target_loc)){
                        int trials = 0;
                        while(!rc.canMove(rand) && trials < 10){
                            rand = RobotPlayer.randomDirection();
                            trials++;
                        }
                        if(rc.canMove(rand)){
                            rc.move(rand);
                        }
                    }
                    else{
                        MapLocation to_move_to = RobotPlayer.invalid_location;
                        while(!rc.canMove(to_move_to)){
                            to_move_to = target_loc.add(RobotPlayer.randomDirection(), RobotPlayer.getSightRadius());
                        }
                        rc.move(to_move_to);
                    }
                }

                //System.out.println("Made it E");

                Clock.yield();

            } catch (Exception e){
                System.out.println("Scout exception");
                e.printStackTrace();
            }
        }
    }
}
