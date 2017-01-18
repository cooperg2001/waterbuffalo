package scoutrushV6;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Gardener {

    static void runGardener(RobotController rc) throws GameActionException {


        int spots_available = 0; // The number of places around our gardener we can build in
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
                float gardener_angle_gradient = 2 * (float)Math.PI/RobotPlayer.num_angles;


                /** insert new code here
                 *
                 */
                boolean should_build_tree = false;
                boolean[] occupied = new boolean[RobotPlayer.num_angles];
                for(int i=0; i < RobotPlayer.num_angles; i++){
                    occupied[i] = false;
                }
                for(int i=0; i < RobotPlayer.num_angles; i++){
                    Direction next_build = RobotPlayer.backward.rotateLeftRads(i*gardener_angle_gradient);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(!rc.canMove(next_build, 2)){
                        occupied[i] = true;
                    }
                }
                for(int i = 0; i < RobotPlayer.num_angles; i++){
                    for(int j=0; j < RobotPlayer.num_angles; j++){
                        if(!occupied[i] && !occupied[j]){
                            int distance = Math.min(j - i, 24 - (j - i));
                            if (distance >= 4){
                                should_build_tree = true;
                            }
                        }
                    }
                }


                if(should_build_tree && rc.getRoundNum() > 40 && rc.getTreeCount() < 3 * rc.readBroadcast(904)){
                    // Leave a location open to build combat units in, and don't build a tree if our army is weak
                    // System.out.println("Building tree...");
                    for(int i = 0; i < RobotPlayer.num_angles; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.backward.rotateLeftRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }
                        else{
                            next_build = RobotPlayer.backward.rotateRightRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }
                        if(rc.canPlantTree(next_build)){
                            rc.plantTree(next_build);
                        }
                    }
                }
                else{
                    int numSoldiers = rc.readBroadcast(904);
                    int numScouts = rc.readBroadcast(903);
                    int numLumberjacks = rc.readBroadcast(902);


                    // Build either a soldier or a scout, depending on the makeup of our army and the game time
                    for(int i = 0; i < RobotPlayer.num_angles; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.forward.rotateLeftRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }
                        else{
                            next_build = RobotPlayer.forward.rotateRightRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }

                        if(numLumberjacks < 2
                                && rc.canBuildRobot(RobotType.LUMBERJACK, next_build)
                                && numScouts + numSoldiers >= 2
                                && RobotPlayer.neutral_trees.length > 0){
                            rc.buildRobot(RobotType.LUMBERJACK, next_build);
                        }
                        if((numSoldiers < 20)
                                && rc.canBuildRobot(RobotType.SOLDIER, next_build)
                                && (numSoldiers < numScouts + 1 || rc.getRoundNum() > 400)){
                            rc.buildRobot(RobotType.SOLDIER, next_build);
                        }
                        if((numScouts < 20)
                                && rc.canBuildRobot(RobotType.SCOUT, next_build)
                                && (numScouts < 2 * numSoldiers + 1)
                                && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
                            //System.out.println("Building scout...");
                            rc.buildRobot(RobotType.SCOUT, next_build);
                        }
                    }
                }
                if(!sentDeathSignal && rc.getHealth() < 8) {
                    sentDeathSignal = true;
                    rc.broadcast(900 + RobotPlayer.typeToInt(rc.getType()), rc.readBroadcast(900 + RobotPlayer.typeToInt(rc.getType())) - 1);
                }
                Clock.yield();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
