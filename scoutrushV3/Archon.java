package scoutrushV3;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Archon {

    static void runArchon(RobotController rc) throws GameActionException {


        if(rc.readBroadcast(900) == 1){
            int encode = (int)RobotPlayer.their_archons[0].x * 1000 + (int)RobotPlayer.their_archons[0].y;
            rc.broadcast(500, encode);
        }
        Direction rand = RobotPlayer.randomDirection();
        int saved_target_id = -1;

        while (true) {

            try {
                //updateSurroundings();
                RobotPlayer.findShakableTrees();
                RobotPlayer.checkForStockpile();
                if(rc.getRobotCount() > 40 || rc.getRoundNum() > 2800 || rc.getTeamBullets() > 10000 - 10 * rc.getTeamVictoryPoints()){
                    rc.donate(rc.getTeamBullets() - (rc.getTeamBullets() % 10));
                }

                int data = rc.readBroadcast(500);
                MapLocation last_seen = new MapLocation(data / 1000, data % 1000);
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.ENEMY);

                if(enemies.length == 0){
                    if(rc.getLocation().distanceTo(last_seen) < RobotPlayer.getSightRadius()){
                        rc.broadcast(500, 0);
                        rc.broadcast(501, 0);
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


                for(int i = 0; i < RobotPlayer.num_slices; i++){
                    Direction dir = RobotPlayer.absolute_right.rotateLeftRads(RobotPlayer.potential_angles[i]);
                    if(rc.canHireGardener(dir) && (rc.readBroadcast(904) >= 2 * rc.readBroadcast(901) - 3 || rc.getTeamBullets() > 200)){
                        if(rc.getRoundNum() > 30){
                            rc.hireGardener(dir);
                        }
                        else if(rc.canMove(dir.rotateLeftDegrees(90))){
                            rc.hireGardener(dir);
                            rc.move(dir.rotateLeftDegrees(90));
                        }
                        else if(rc.canMove(dir.rotateRightDegrees(90))){
                            rc.hireGardener(dir);
                            rc.move(dir.rotateRightDegrees(90));
                        }
                        break;
                    }
                }

                int trials = 0;
                while(!rc.canMove(rand) && trials < 10){
                    rand = RobotPlayer.randomDirection();
                    trials++;
                }
                if(rc.canMove(rand) && !rc.hasMoved()){
                    rc.move(rand);
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
}
