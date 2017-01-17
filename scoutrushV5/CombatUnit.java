package scoutrushV5;

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
        boolean sentDeathSignal = false; //have we sent out a death signal
        Direction rand = RobotPlayer.randomDirection();

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

                int totalEnemies = 0;
                for(int i = 0; i < RobotPlayer.enemies.length; i++) {
                    totalEnemies += RobotPlayer.enemies[i].length;
                }

                if(totalEnemies > 0){
                    RobotInfo priority_target = RobotPlayer.get_priority_target();
                    if(RobotPlayer.shotWillHit(rc.getLocation(), priority_target)){
                        boolean should_shoot = true;
                        /*if(priority_target.getType() == RobotType.ARCHON){
                            if(rc.getRoundNum() < 500){
                                should_shoot = false;
                            }
                        }*/
                        if(rc.getType() == RobotType.SCOUT && (priority_target.getType() == RobotType.SCOUT)){
                            if(RobotPlayer.robots.length - totalEnemies < 3){
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

                //DEATH SIGNAL
                if(!sentDeathSignal && rc.getHealth() < 8) {
                    sentDeathSignal = true;
                    if(rc.getType() == RobotType.SCOUT){
                        rc.broadcast(904, rc.readBroadcast(904) - 1); // Decrement the number of scouts we've created this game
                    }
                    else{
                        rc.broadcast(903, rc.readBroadcast(903) - 1); // Decrement the number of soldiers we've created this game
                    }
                }

                Clock.yield();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
