package scoutrushV4;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Gardener {

    static void runGardener(RobotController rc) throws GameActionException {


        rc.broadcast(901, rc.readBroadcast(901) + 1); // Increment the number of gardeners our team has created
        int spots_available = 0; // The number of places around our gardener we can build in

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

                TreeInfo[] tree_list = rc.senseNearbyTrees(-1, rc.getTeam()); // Find all allied trees within range
                if(tree_list.length > 0){
                    // Determine tree in range with least health
                    TreeInfo best = tree_list[0];
                    for(int i = 0; i < tree_list.length; i++){
                        if(tree_list[i].getHealth() < best.getHealth() && rc.canWater(tree_list[i].getLocation())){
                            best = tree_list[i];
                        }
                    }
                    // Water the tree in range with least health, if one exists
                    if(rc.canWater(best.getLocation())){
                        rc.water(best.getLocation());
                    }
                }

                // Determine the number of available build spots
                spots_available = 0;
                int num_gardener_slices = 6;
                for(int i = 0; i < num_gardener_slices; i++){
                    Direction next_build = RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(rc.canMove(next_build, 2)){
                        spots_available++;
                    }
                }

                if(spots_available > 1 && rc.getRoundNum() > 40 && rc.getTreeCount() < 3 * rc.readBroadcast(904)){
                    // Leave a location open to build combat units in, and don't build a tree if our army is weak
                    // System.out.println("Building tree...");
                    for(int i = 0; i < num_gardener_slices; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.backward.rotateLeftRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
                        }
                        else{
                            next_build = RobotPlayer.backward.rotateRightRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
                        }
                        if(rc.canPlantTree(next_build)){
                            rc.plantTree(next_build);
                        }
                    }
                }
                else{
                    // Build either a soldier or a scout, depending on the makeup of our army and the game time
                    for(int i = 0; i < num_gardener_slices; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.forward.rotateLeftRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
                        }
                        else{
                            next_build = RobotPlayer.forward.rotateRightRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
                        }
                        if((rc.readBroadcast(903) < 20) && rc.canBuildRobot(RobotType.SOLDIER, next_build) && (rc.readBroadcast(903) < rc.readBroadcast(904) || rc.getRoundNum() > 400)){
                            rc.buildRobot(RobotType.SOLDIER, next_build);
                        }
                        if((rc.readBroadcast(904) < 20) && rc.canBuildRobot(RobotType.SCOUT, next_build) && rc.readBroadcast(904) < 2 * rc.readBroadcast(903) + 1 && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
                            //System.out.println("Building scout...");
                            rc.buildRobot(RobotType.SCOUT, next_build);
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
