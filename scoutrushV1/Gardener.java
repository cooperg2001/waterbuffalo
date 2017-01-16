package scoutrushV1;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Gardener {

    static void runGardener(RobotController rc) throws GameActionException {
        rc.broadcast(901, rc.readBroadcast(901) + 1);

        int spots_available = 0;

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                //updateSurroundings();
                //findShakableTrees();

                spots_available = 0;
                for(int i = 0; i < RobotPlayer.num_slices; i++){
                    Direction next_build = RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_slices);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(!rc.isCircleOccupied(rc.getLocation().add(next_build,2), 1)){
                        spots_available++;
                    }
                }

                //System.out.println("There are " + spots_available + " spots available from " + rc.getLocation());

                if(spots_available > 1 && rc.getRoundNum() > 40){
                    //System.out.println("Building tree...");
                    for(int i = 0; i < RobotPlayer.num_slices; i++){
                        if(rc.canPlantTree(RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_slices))){
                            rc.plantTree(RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_slices));
                        }
                    }
                }
                else{
                    for(int i = 0; i < RobotPlayer.num_slices; i++){
                        if((rc.readBroadcast(904) < 20 || rc.getTeamBullets() > 300) && rc.canBuildRobot(RobotType.SCOUT, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_slices))){
                            //System.out.println("Building scout...");
                            rc.buildRobot(RobotType.SCOUT, RobotPlayer.absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/RobotPlayer.num_slices));
                        }
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


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
}
