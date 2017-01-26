package soldierrushV4;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Gardener {

    static final float gardener_angle_gradient = 2 * (float)Math.PI/RobotPlayer.num_angles;

    static void runGardener(RobotController rc) throws GameActionException {

        Direction rand = RobotPlayer.randomDirection();
        boolean sentDeathSignal = false; // true if we've sent a death signal
        int round_spawned = rc.getRoundNum();
        while(rc.getRoundNum() > 200 && rc.senseNearbyTrees(4).length > 0 && round_spawned + 200 > rc.getRoundNum()){
            if (rc.canMove(rand) && !rc.hasMoved()) {
                rc.move(rand);
            } else {
                int trials = 0;
                while (!rc.canMove(rand) && trials < 10) {
                    rand = RobotPlayer.randomDirection();
                    trials++;
                }
                if (rc.canMove(rand) && !rc.hasMoved()) {
                    rc.move(rand);
                }
            }
            Clock.yield();
        }

        while(true){
            try{
                RobotPlayer.findShakableTrees();
                RobotPlayer.checkForStockpile();

                RobotPlayer.robots = rc.senseNearbyRobots();
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

                /**
                 * Test for space to build trees, and units afterward
                 */

                boolean should_build_tree = false;
                boolean[] occupied = new boolean[RobotPlayer.num_angles];
                for(int i=0; i < RobotPlayer.num_angles; i++){
                    occupied[i] = false;
                }
                for(int i=0; i < RobotPlayer.num_angles; i++){
                    Direction next_build = RobotPlayer.backward.rotateLeftRads(i*gardener_angle_gradient);
                    //System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
                    if(!rc.canPlantTree(next_build)){
                        occupied[i] = true;
                    }
                }
                for(int i = 0; i < RobotPlayer.num_angles; i++){
                    for(int j=0; j < RobotPlayer.num_angles; j++){
                        if(!occupied[i] && !occupied[j]){
                            int distance = Math.min(j - i, RobotPlayer.num_angles - (j - i));
                            if (distance >= RobotPlayer.num_angles / 6){
                                should_build_tree = true;
                            }
                        }
                    }
                }

                if(should_build_tree &&  rc.getRoundNum() > 40
                        && (RobotPlayer.our_trees.length < gardener_ct * RobotPlayer.getTreeToGardenerRatio()
                            || tree_ct * RobotPlayer.getArmyToTreeRatio() < (soldier_ct+lumberjack_ct+tank_ct)
                            || rc.getTeamBullets() > 150)){
                    // Leave a location open to build combat units in, and don't build a tree if our army is weak
                    // System.out.println("Building tree...");
                    for(int i = 0; i < RobotPlayer.num_angles; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.backward.rotateLeftRads(i/2 * gardener_angle_gradient);
                        }
                        else{
                            next_build = RobotPlayer.backward.rotateRightRads((i+1)/2 * gardener_angle_gradient);
                        }
                        if(rc.canPlantTree(next_build)){
                            rc.plantTree(next_build);
                        }
                    }
                }
                else{
                    // Build either a soldier or a scout, depending on the makeup of our army and the game time
                    for(int i = 0; i < RobotPlayer.num_angles; i++){
                        Direction next_build;
                        if(i % 2 == 0){
                            next_build = RobotPlayer.forward.rotateLeftRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }
                        else{
                            next_build = RobotPlayer.forward.rotateRightRads((int)(i + 1)/2 * gardener_angle_gradient);
                        }

						
						if(tank_ct < 20
						        && rc.canBuildRobot(RobotType.TANK, next_build)
								&& (rc.getRoundNum() > 1000 || rc.getTeamBullets() > 300 || soldier_ct > 6)
								&& (rc.getTeamBullets() > 6 * soldier_ct)){
							rc.buildRobot(RobotType.TANK, next_build);
						}
                        if(lumberjack_ct < 20
                                && rc.canBuildRobot(RobotType.LUMBERJACK, next_build)
                                && (soldier_ct >= 3 * lumberjack_ct + 2
                                    || rc.senseNearbyTrees(-1, RobotPlayer.NEUTRAL).length > 0)
                                && rc.getRoundNum() < 1000){
                            rc.buildRobot(RobotType.LUMBERJACK, next_build);
                            rc.broadcast(902, rc.readBroadcast(902) + 1);
                        }

                        if((soldier_ct < 20)
                                && rc.canBuildRobot(RobotType.SOLDIER, next_build)
								&& rc.getRoundNum() < 1000
								&& rc.getTeamBullets() > 6 * soldier_ct){
                            rc.buildRobot(RobotType.SOLDIER, next_build);
                            rc.broadcast(904, rc.readBroadcast(904) + 1);
                        }
                        /*if(scout_ct < 10
                                && rc.getOpponentVictoryPoints() + 40 > rc.getRoundNum() * 10000/700
                                && rc.canBuildRobot(RobotType.SCOUT, next_build)){
                            rc.buildRobot(RobotType.SCOUT, next_build);
                        }*/
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
