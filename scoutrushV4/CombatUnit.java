package scoutrushV4;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class CombatUnit {

    static void runCombatUnit(RobotController rc) throws GameActionException {

        if(rc.getType() == RobotType.SCOUT){
            rc.broadcast(904, rc.readBroadcast(904) + 1); // Increment the number of scouts we've created this game
        }
        else{
            rc.broadcast(903, rc.readBroadcast(903) + 1); // Increment the number of soldiers we've created this game
        }

        Direction rand = RobotPlayer.randomDirection();

        while(true){
            try{
                RobotPlayer.findShakableTrees();
                RobotPlayer.checkForStockpile();

                RobotPlayer.robots = rc.senseNearbyRobots();
                RobotPlayer.enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);
                RobotPlayer.trees = rc.senseNearbyTrees(-1);
                RobotPlayer.neutral_trees = rc.senseNearbyTrees(-1, RobotPlayer.NEUTRAL);
                RobotPlayer.their_trees = rc.senseNearbyTrees(-1, RobotPlayer.ENEMY);
                RobotPlayer.our_trees = rc.senseNearbyTrees(-1, RobotPlayer.FRIEND);
                RobotPlayer.bullets = rc.senseNearbyBullets();
                int last_sighting_location_encoded = rc.readBroadcast(500);
                RobotPlayer.last_sighting_location = new MapLocation(last_sighting_location_encoded / 1000, last_sighting_location_encoded % 1000);
                int last_sighting_type_encoded = rc.readBroadcast(501);
                RobotPlayer.last_sighting_type = RobotPlayer.intToType(last_sighting_type_encoded);

                RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY); // find all nearby enemies

                if(enemies.length == 0){
                    // No enemies found
                    if(rc.getLocation().distanceTo(RobotPlayer.last_sighting_location) < rc.getType().sensorRadius){
                        // There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
                        rc.broadcast(500, 999999);
                        rc.broadcast(501, 0);
                    }
                }
                else{
                    // Enemies found, update last sighting to first enemy seen
                    int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
                    rc.broadcast(500, encode);
                    rc.broadcast(501, RobotPlayer.typeToInt(enemies[0].getType()));
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

                if(enemies.length > 0){
                    RobotInfo priority_target = RobotPlayer.get_priority_target();
                    if(RobotPlayer.shotWillHit(rc.getLocation(), priority_target)){
                        boolean should_shoot = true;
                        if(priority_target.getType() == RobotType.ARCHON){
                            if(rc.getRoundNum() < 500){
                                should_shoot = false;
                            }
                        }
                        if(rc.getType() == RobotType.SCOUT && (priority_target.getType() == RobotType.SCOUT)){
                            if(RobotPlayer.robots.length - enemies.length < 3){
                                should_shoot = false;
                            }
                        }
                        if(rc.getType() == RobotType.SOLDIER){
                            if(priority_target.getType() == RobotType.SCOUT && rc.getLocation().distanceTo(priority_target.getLocation()) > 5){
                                should_shoot = false;
                            }
                        }
                        if(rc.canFirePentadShot() && should_shoot){
                            rc.firePentadShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                        if(rc.canFireTriadShot() && should_shoot){
                            rc.fireTriadShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                        if(rc.canFireSingleShot() && should_shoot){
                            rc.fireSingleShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                    }
                }


                Clock.yield();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
