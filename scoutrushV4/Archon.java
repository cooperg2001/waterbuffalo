package scoutrushV4;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Archon {

    static void runArchon(RobotController rc) throws GameActionException {


        rc.broadcast(900, rc.readBroadcast(900) + 1);
        if(rc.readBroadcast(900) == 1){
            // This is our first archon; initialize an enemy target for combat units to orient towards
            int encode = (int)RobotPlayer.their_archons[0].x * 1000 + (int)RobotPlayer.their_archons[0].y;
            rc.broadcast(500, encode);
            rc.broadcast(501, RobotPlayer.typeToInt(RobotType.ARCHON));
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

                // Check all angles around us for potential build locations
                for(int i = 0; i < RobotPlayer.num_angles; i++){
                    Direction dir = RobotPlayer.absolute_right.rotateLeftRads(RobotPlayer.potential_angles[i]);
                    if(rc.canHireGardener(dir) && (rc.readBroadcast(904) >= 2 * rc.readBroadcast(901) - 2 || rc.getTeamBullets() > 200)){
                        // We can hire a gardener, and we have a sufficiently big army to justify hiring gardeners
                        // Try to make sure hiring a gardener doesn't trap our archon
                        if(rc.getRoundNum() > 30){
                            rc.hireGardener(dir);
                        }
                        else if(rc.canMove(dir.rotateLeftDegrees(90))){
                            rc.hireGardener(dir);
                            rc.move(dir.rotateLeftDegrees(90));
                        }
                        else if(rc.canMove(dir.rotateRightDegrees(90))){
                            rc.hireGardener(dir);
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
