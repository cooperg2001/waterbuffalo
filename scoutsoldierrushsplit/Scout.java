package scoutsoldierrushsplit;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Scout {

    static void runScout(RobotController rc) throws GameActionException {

        Direction rand = RobotPlayer.randomDirection();
        MapLocation secondary_target = RobotPlayer.invalid_location;
        int patience = 0;
        RobotInfo target_robot = null;
        int target_id = -1;
        MapLocation target_loc = RobotPlayer.invalid_location;
        int saved_target_id = -1;

        while(true) {

            try{
                //System.out.println("Before updating surroundings: " + Clock.getBytecodeNum());
                //updateSurroundings();
                //System.out.println("After updating surroundings: " + Clock.getBytecodeNum());
                RobotPlayer.findShakableTrees();

                System.out.println("My beginning-of-turn secondary target is... " + secondary_target);

                if(RobotPlayer.isValidLoc(secondary_target)){
                    patience++;
                    if(rc.getLocation().distanceTo(secondary_target) < RobotPlayer.getSightRadius()){
                        rc.broadcast(500, 0);
                        secondary_target = RobotPlayer.invalid_location;
                    }
                    if(patience == 40){
                        secondary_target = RobotPlayer.invalid_location;
                        patience = 0;
                    }
                }

                int last_seen = rc.readBroadcast(500);
                int responders = rc.readBroadcast(501);

                if(!RobotPlayer.isValidLoc(secondary_target) || (last_seen != 0 && patience >= 10)){
                    patience = 0;
                    if(last_seen != 0 && responders < 5){
                        rc.broadcast(501, rc.readBroadcast(501) + 1);
                        secondary_target = new MapLocation(last_seen / 1000, last_seen % 1000);
                    }
                }

                System.out.println("Now my secondary target is... " + secondary_target);

                //System.out.println("Made it A");

                RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);

                boolean targetAlive = false;
                for(int i = 0; i < enemies.length; i++){
                    if(enemies[i].getID() == target_id){
                        targetAlive = true;
                        target_loc = enemies[i].getLocation();
                        target_robot = enemies[i];
                    }
                }

                //System.out.println("Made it B");

                if(!targetAlive){
                    target_id = -1;
                    target_loc = RobotPlayer.invalid_location;
                    target_robot = null;
                }

                System.out.println("My target has moved to " + target_loc);

                if(enemies.length > 0){
                    RobotInfo best_robot;
                    if(target_id != -1){
                        best_robot = target_robot;
                    }
                    else{
                        best_robot = enemies[0];
                    }
                    if(rc.getType() == RobotType.SCOUT){
                        for(int i = 0; i < enemies.length; i++){
                            if(best_robot.getType() == enemies[i].getType()){
                                if(enemies[i].getHealth() < best_robot.getHealth()){
                                    best_robot = enemies[i];
                                }
                            }
                            else if(best_robot.getType() != RobotType.GARDENER && enemies[i].getType() == RobotType.GARDENER){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.SOLDIER && enemies[i].getType() == RobotType.SOLDIER){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.LUMBERJACK && enemies[i].getType() == RobotType.LUMBERJACK){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.ARCHON && enemies[i].getType() == RobotType.ARCHON){
                                best_robot = enemies[i];
                            }
                        }
                    }
                    else{
                        for(int i = 0; i < enemies.length; i++){
                            if(best_robot.getType() == enemies[i].getType()){
                                if(enemies[i].getHealth() < best_robot.getHealth()){
                                    best_robot = enemies[i];
                                }
                            }
                            else if(best_robot.getType() != RobotType.SCOUT && enemies[i].getType() == RobotType.SCOUT){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.SOLDIER && enemies[i].getType() == RobotType.SOLDIER){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.LUMBERJACK && enemies[i].getType() == RobotType.LUMBERJACK){
                                best_robot = enemies[i];
                            }
                            else if(best_robot.getType() != RobotType.ARCHON && enemies[i].getType() == RobotType.ARCHON){
                                best_robot = enemies[i];
                            }
                        }
                    }
                    target_robot = best_robot;
                    target_id = target_robot.getID();
                    target_loc = target_robot.getLocation();
                }

                if(target_id != -1 && rc.getRoundNum() < 200){
                    if(rc.getType() == RobotType.SCOUT && (target_robot.getType() == RobotType.ARCHON || target_robot.getType() == RobotType.SCOUT)){
                        target_id = -1;
                        target_loc = RobotPlayer.invalid_location;
                        target_robot = null;
                    }
                    if(rc.getType() == RobotType.SOLDIER && (target_robot.getType() == RobotType.ARCHON)){
                        target_id = -1;
                        target_loc = RobotPlayer.invalid_location;
                        target_robot = null;
                    }
                }

                if(target_id != -1){
                    int encode = (int)target_robot.getLocation().x * 1000 + (int)target_robot.getLocation().y;
                    rc.broadcast(500, encode);
                    if(target_id != saved_target_id){
                        rc.broadcast(501, 1);
                        saved_target_id = target_id;
                    }
                }

                //System.out.println("Made it C");

                MapLocation target;

                if(target_id != -1){
                    target = RobotPlayer.getBestShootingLocation(target_robot);
                }
                else{
                    target = RobotPlayer.invalid_location;
                }
				/*if(rc.getRoundNum() > 100 && rc.getRoundNum() < 150 && target_id != -1){
					System.out.println(rc.getLocation() + " " + target_robot.getType() + " " + target);
				}*/

                //System.out.println("Made it D");

                System.out.println("I am at " + rc.getLocation());
                System.out.println("I want to move to " + target);

                if(RobotPlayer.isValidLoc(target)){
                    System.out.println("Fortunately " + target + " is valid");
                    //System.out.println("Target acquired... " + Clock.getBytecodeNum());
                    if(rc.canMove(target)){
                        System.out.println("I indeed can move to " + target);
                        rc.move(target);
                        System.out.println("I have now moved to " + rc.getLocation());
                        if(rc.getLocation().distanceTo(target) < 0.5 || (rc.getLocation().distanceTo(target) < 1.5 && rc.getType() == RobotType.SOLDIER)){
                            if(rc.canFirePentadShot()){
                                rc.firePentadShot(rc.getLocation().directionTo(target_loc));
                            }
                            else if(rc.canFireTriadShot()){
                                rc.fireTriadShot(rc.getLocation().directionTo(target_loc));
                            }
                            else if(rc.canFireSingleShot()){
                                rc.fireSingleShot(rc.getLocation().directionTo(target_loc));
                            }
                        }
                    }
                    else{
                        System.out.println("But sadly, I cannot move to " + target);
                        if(RobotPlayer.isValidLoc(secondary_target) && rc.canMove(secondary_target)){
                            rc.move(secondary_target);
                            rand = RobotPlayer.randomDirection();
                        }
                        else{
                            int trials = 0;
                            while((!rc.canMove(rand) || RobotPlayer.willBeHit(rand)) && trials < 10){
                                rand = RobotPlayer.randomDirection();
                                trials++;
                            }
                            if(rc.canMove(rand)){
                                rc.move(rand);
                            }
                        }
                    }
                }
                else{
                    TreeInfo[] trees = rc.senseNearbyTrees(-1, RobotPlayer.NEUTRAL);
                    TreeInfo[] trees2 = rc.senseNearbyTrees(-1, RobotPlayer.ENEMY);
                    if(rc.getRoundNum() > 1500 && (trees.length > 0 || trees2.length > 0)){
                        TreeInfo best;
                        if(trees.length > 0){
                            best = trees[0];
                        }
                        else{
                            best = trees2[0];
                        }
                        for(int i = 0; i < trees.length; i++){
                            if(rc.getLocation().distanceTo(trees[i].getLocation()) < rc.getLocation().distanceTo(best.getLocation())){
                                best = trees[i];
                            }
                        }
                        for(int i = 0; i < trees2.length; i++){
                            if(rc.getLocation().distanceTo(trees2[i].getLocation()) < rc.getLocation().distanceTo(best.getLocation())){
                                best = trees2[i];
                            }
                        }
                        if(rc.canMove(best.getLocation())){
                            rc.move(best.getLocation());
                            if(rc.canFirePentadShot()){
                                rc.firePentadShot(rc.getLocation().directionTo(best.getLocation()));
                            }
                            else if(rc.canFireTriadShot()){
                                rc.fireTriadShot(rc.getLocation().directionTo(best.getLocation()));
                            }
                            else if(rc.canFireSingleShot()){
                                rc.fireSingleShot(rc.getLocation().directionTo(best.getLocation()));
                            }
                        }
                        else{
                            int trials = 0;
                            while((!rc.canMove(rand) || RobotPlayer.willBeHit(rand)) && trials < 10){
                                rand = RobotPlayer.randomDirection();
                                trials++;
                            }
                            if(rc.canMove(rand)){
                                rc.move(rand);
                            }
                        }
                    }
                    else if(!RobotPlayer.isValidLoc(target_loc) || rc.getType() == RobotType.SOLDIER){
                        if(RobotPlayer.isValidLoc(secondary_target) && rc.canMove(secondary_target)){
                            rc.move(secondary_target);
                            rand = RobotPlayer.randomDirection();
                        }
                        else{
                            int trials = 0;
                            while((!rc.canMove(rand) || RobotPlayer.willBeHit(rand)) && trials < 10){
                                rand = RobotPlayer.randomDirection();
                                trials++;
                            }
                            if(rc.canMove(rand)){
                                rc.move(rand);
                            }
                        }
                    }
                    else{
                        MapLocation to_move_to = RobotPlayer.invalid_location;
                        while(!rc.canMove(to_move_to)){
                            to_move_to = target_loc.add(RobotPlayer.randomDirection(), RobotPlayer.getSightRadius());
                        }
                        rc.move(to_move_to);
                    }
                }

                //System.out.println("Made it E");

                Clock.yield();

            } catch (Exception e){
                System.out.println("Scout exception");
                e.printStackTrace();
            }
        }
    }
}
