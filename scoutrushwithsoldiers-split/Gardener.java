package scoutrushwithsoldiers;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Gardener {

    static void runGardener(RobotController rc) throws GameActionException {


        int spots_available = 0;
        int saved_target_id = -1;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                //updateSurroundings();
                //findShakableTrees();
                int data = rc.readBroadcast(500);
                MapLocation last_seen = new MapLocation(data / 1000, data % 1000);
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);

                if(enemies.length == 0){
                    if(rc.getLocation().distanceTo(last_seen) < RobotPlayer.getSightRadius()){
                        rc.broadcast(500, 0);
                    }
                }
                else{
                    int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
                    rc.broadcast(500, encode);
                    if(enemies[0].getID() != saved_target_id){
                        saved_target_id = enemies[0].getID();
                        rc.broadcast(501, 0);
                    }
                }

                TreeInfo[] tree_list = rc.senseNearbyTrees(-1, rc.getTeam());
                if(tree_list.length > 0){
                    TreeInfo best = tree_list[0];
                    for(int i = 0; i < tree_list.length; i++){
                        if(tree_list[i].getHealth() < best.getHealth() && rc.canWater(tree_list[i].getLocation())){
                            best = tree_list[i];
                        }
                    }
                    if(rc.canWater(best.getLocation())){
                        rc.water(best.getLocation());
                    }
                }

                spots_available = 0;
                for(int i = 0; i < RobotPlayer.num_gardener_slices; i++){
                    Direction next_build = RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(rc.canMove(next_build, 2)){
                        spots_available++;
                    }
                }

                //System.out.println("There are " + spots_available + " spots available from " + rc.getLocation());

                if(spots_available > 1 && rc.getRoundNum() > 40 && rc.getTreeCount() < 3 * rc.readBroadcast(904)){
                    //System.out.println("Building tree...");
                    for(int i = 0; i < RobotPlayer.num_gardener_slices; i++){
                        if(rc.canPlantTree(RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices))){
                            rc.plantTree(RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices));
                        }
                    }
                }
                else{
                    for(int i = 0; i < RobotPlayer.num_gardener_slices; i++){
                        if((rc.readBroadcast(903) < 20) && rc.canBuildRobot(RobotType.SOLDIER, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices)) && (rc.readBroadcast(903) < rc.readBroadcast(904) + 1 || rc.getRoundNum() > 400)){
                            rc.buildRobot(RobotType.SOLDIER, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices));
                        }
                        if((rc.readBroadcast(904) < 20) && rc.canBuildRobot(RobotType.SCOUT, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices)) && rc.readBroadcast(904) < 3 * rc.readBroadcast(903) + 3 && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
                            //System.out.println("Building scout...");
                            rc.buildRobot(RobotType.SCOUT, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_gardener_slices));
                        }
                    }
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
}
