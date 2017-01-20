package scoutrushV7;

import battlecode.common.*;
import java.util.List;
import java.util.ArrayList;


public class Gardener {

    static void runGardener(RobotController rc) throws GameActionException {


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



                if(RobotPlayer.our_trees.length > 0){
                    // Determine tree in range with least health
                    TreeInfo best = RobotPlayer.our_trees[0];
                    for(int i = 0; i < RobotPlayer.our_trees.length; i++){
                        if(RobotPlayer.our_trees[i].getHealth() < best.getHealth() && rc.canWater(RobotPlayer.our_trees[i].getLocation())){
                            best = RobotPlayer.our_trees[i];
                        }
                    }
                    // Water the tree in range with least health, if one exists
                    if(rc.canWater(best.getLocation())){
                        rc.water(best.getLocation());
                    }
                }

                // Determine the number of available build spots
                float gardener_angle_gradient = 2 * (float)Math.PI/RobotPlayer.num_angles;


                boolean should_build_tree = false;
                List<Integer> unoccupied_slots = new ArrayList<Integer>();
                for(int i=0; i < RobotPlayer.num_angles; i++){
                    Direction next_build = RobotPlayer.backward.rotateLeftRads(i*gardener_angle_gradient);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(!rc.canMove(next_build, 2)){
                        continue;
                    }
                    else{
                        unoccupied_slots.add(i);
                    }
                }
                if (unoccupied_slots.size() >= 6){
                    should_build_tree = true;
                }
                else{
                    for(int i = 0; i < unoccupied_slots.size(); i++) {
                        for (int j = 0; j < unoccupied_slots.size(); j++) {
                            int iVal = unoccupied_slots.get(i);
                            int jVal = unoccupied_slots.get(j);
                            int distance = Math.min(jVal - iVal, 24 - (jVal - iVal));
                            if (distance >= 5){
                                should_build_tree = true;
                            }
                        }
                    }
                }

                if(should_build_tree && rc.getRoundNum() > 40 && (2 * rc.getTreeCount() + 1 < (rc.readBroadcast(904) + rc.readBroadcast(903)) || rc.getTeamBullets() > 100)){
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

                        if( numLumberjacks < 2
                                && rc.canBuildRobot(RobotType.LUMBERJACK, next_build)
                                && RobotPlayer.neutral_trees.length > 0){
                            rc.buildRobot(RobotType.LUMBERJACK, next_build);
                            rc.broadcast(902, rc.readBroadcast(902) + 1);
                        }
                        if((numSoldiers < 20)
                                && rc.canBuildRobot(RobotType.SOLDIER, next_build)
                                && (numSoldiers < numScouts || rc.getRoundNum() > 400)){
                            rc.buildRobot(RobotType.SOLDIER, next_build);
                            rc.broadcast(904, rc.readBroadcast(904) + 1);
                        }
                        if((numScouts < 20)
                                && rc.canBuildRobot(RobotType.SCOUT, next_build)
                                && (numScouts < 2 * numSoldiers + 1)
                                && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
                            //System.out.println("Building scout...");
                            rc.buildRobot(RobotType.SCOUT, next_build);
                            rc.broadcast(903, rc.readBroadcast(903) + 1);
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
