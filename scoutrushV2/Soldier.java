package scoutrushV2;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Soldier {

    static void runSoldier(RobotController rc) throws GameActionException {

        System.out.println("I'm a soldier!");
        Direction rand = RobotPlayer.randomDirection();
        boolean hasTarget = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);
        MapLocation target_loc = RobotPlayer.invalid_location;
        Direction target_dir = RobotPlayer.forward;
        int patience=0;
        boolean fire_away = false;
        final int END_OF_PATIENCE=15;

        while (true) {
            enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);
            try {

                if(!hasTarget) {
                    if(enemies.length == 0) { //just wander
                        int trials = 0;
                        while (!rc.canMove(rand) && trials < 10) {
                            rand = RobotPlayer.randomDirection();
                            trials++;
                        }
                        if (rc.canMove(rand)) {
                            rc.move(rand);
                        }

                        Clock.yield();
                    } else { //we found a target!
                        hasTarget=true;
                    }
                }
                if(hasTarget) {
                    if(enemies.length==0) {//we lost it :( keep moving but have limited patience
                        patience++;
                        if(patience > END_OF_PATIENCE) {hasTarget=false;} //give up
                        if(rc.canMove(target_dir, 1)) {
                            rc.move(target_dir, 1);
                        }
                        Clock.yield();
                    } else { //move towards closest enemy and shoot at it
                        patience=0;
                        int priority = 0;
                        for(int i = 1; i < enemies.length; i++) {
                            if(rc.getLocation().distanceTo(enemies[i].getLocation()) < rc.getLocation().distanceTo(enemies[priority].getLocation())) {
                                priority=i;
                            }
                        }
                        target_loc=enemies[priority].getLocation();
                        target_dir=rc.getLocation().directionTo(target_loc);
                        /*if(rc.canMove(target_dir, 1)) {
                            rc.move(target_dir, 1);
                        } else if(rc.canMove(target_dir, (float)0.1)) {
                            rc.move(target_dir, (float)0.1);*/
                        if(rc.canMove(target_dir, Math.max((float)0.1, rc.getLocation().distanceTo(target_loc)-2))) { //-2 because it's edge-to-edge
                            rc.move(target_dir, rc.getLocation().distanceTo(target_loc)-2);
                        } else if (rc.getLocation().distanceTo(target_loc) > 3.2){ //there's probably something in the way! so wander once...
                            int trials = 0;                                        //this # makes it so it works on archons too (basically must be between 3 and 4)
                            rand = RobotPlayer.randomDirection();
                            while (!rc.canMove(rand) && trials < 10) {
                                rand = RobotPlayer.randomDirection();
                                trials++;
                            }
                            if (rc.canMove(rand)) {
                                rc.move(rand);
                            }
                            fire_away=false;
                        } else {
                            fire_away=true;
                        }

                        if(fire_away && rc.canFireSingleShot()) {
                            rc.fireSingleShot(rc.getLocation().directionTo(target_loc));
                        }
                        Clock.yield();
                    }
                }

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
}
