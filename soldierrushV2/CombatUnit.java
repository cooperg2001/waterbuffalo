package soldierrushV2;

import battlecode.common.*;


public class CombatUnit {

    static void runCombatUnit(RobotController rc) throws GameActionException {

        boolean sentDeathSignal = false; //have we sent out a death signal
        Direction rand = RobotPlayer.randomDirection();

        Direction bugPath_previous_velocity = rand;
        MapLocation bugPath_path_start = rc.getLocation();
        MapLocation bugPath_closest_point = rc.getLocation();
        MapLocation bugPath_path_destination;

        MapLocation target;
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

                target = RobotPlayer.get_best_location();
                rc.setIndicatorLine(rc.getLocation(), target, 255, 0, 0);
                if(rc.getType() == RobotType.SCOUT) {
                    if (target.x != RobotPlayer.INVALID_LOCATION.x && rc.canMove(target) && !rc.hasMoved()) {
                        rc.move(target);
                        rand = RobotPlayer.randomDirection();
                    } else {
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
                    }
                //else we are a soldier or tank
                } else {
                        ////////////////////////
                        // Bugpath to location
                        bugPath_path_destination = target;
                        MapLocation[] startAndFinish = RobotPlayer.bugPathToLoc(
                                bugPath_previous_velocity,
                                bugPath_path_start,
                                bugPath_path_destination,
                                bugPath_closest_point);
                        MapLocation bugPath_prev_point = startAndFinish[0];
                        MapLocation bugPath_curr_point = startAndFinish[1];
                        if (bugPath_prev_point == null
                                || bugPath_curr_point == null
                                || (bugPath_prev_point.x == bugPath_curr_point.x && bugPath_prev_point.y == bugPath_curr_point.y)) {
                            bugPath_path_start = rc.getLocation();
                        } else {
                            bugPath_previous_velocity = bugPath_prev_point.directionTo(bugPath_curr_point);
                            if (RobotPlayer.DistanceToLine(bugPath_curr_point, bugPath_path_start, bugPath_path_destination) < 0.5
                                    && bugPath_curr_point.distanceTo(target) < bugPath_closest_point.distanceTo(target)) {
                                bugPath_closest_point = bugPath_curr_point;
                            }
                        }
                        /////////////////////////
                }

                int totalEnemies = 0;
                for(int i = 0; i < RobotPlayer.enemies.length; i++) {
                    totalEnemies += RobotPlayer.enemies[i].length;
                }

                if(totalEnemies > 0){
                    RobotInfo priority_target = RobotPlayer.get_priority_target();
                    if(RobotPlayer.shotWillHit(rc.getLocation(), priority_target)){
                        boolean should_shoot = true;
                        /*if(priority_target.getType() == RobotType.ARCHON){
                            if(rc.getRoundNum() < 300){
                                should_shoot = false;
                            }
                        }*/
                        /*if(rc.getType() == RobotType.SCOUT && (priority_target.getType() == RobotType.SCOUT)){
                            if(RobotPlayer.robots.length - totalEnemies < 3){
                                should_shoot = false;
                            }
                        }*/
                        if(rc.getType() == RobotType.SOLDIER){
                            if(priority_target.getType() == RobotType.SCOUT && rc.getLocation().distanceTo(priority_target.getLocation()) > 5){
                                should_shoot = false;
                            }
                        }
                        float dist = rc.getLocation().distanceTo(priority_target.getLocation());
                        if(rc.canFirePentadShot() && should_shoot && dist < 5 && rc.getTeamBullets() > 20){
                            rc.firePentadShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                        if(rc.canFireTriadShot() && should_shoot && dist < 5){
                            rc.fireTriadShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                        if(rc.canFireSingleShot() && should_shoot){
                            rc.fireSingleShot(rc.getLocation().directionTo(priority_target.getLocation()));
                        }
                    }
                }

                //DEATH SIGNAL
                if(!sentDeathSignal && rc.getHealth() < 8) {
                    sentDeathSignal = true;
                    rc.broadcast(900 + RobotPlayer.typeToInt(rc.getType()), rc.readBroadcast(900 + RobotPlayer.typeToInt(rc.getType())) - 1);
                    // Decrement the number in our army stored in broadcast
                }

                Clock.yield();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
