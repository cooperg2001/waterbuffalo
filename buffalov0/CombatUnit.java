package buffalov0;

import battlecode.common.*;


public class CombatUnit {

    static void runCombatUnit(RobotController rc) throws GameActionException {

        boolean sentDeathSignal = false; //have we sent out a death signal
        Direction rand = RobotPlayer.randomDirection();

        RobotInfo target;
        MapLocation targetLoc;
        MapLocation prevLoc = null, nextLoc;
        MapLocation battleStance;
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

                target = CombatStrategy.getShootingTarget();
                if(target == null || (target.getType() == RobotType.ARCHON && rc.getRoundNum() < 499)){
                    prevLoc = null;
                    targetLoc = CombatStrategy.locateTarget();
                    if (!targetLoc.equals(RobotPlayer.INVALID_LOCATION)) {
                        RobotPlayer.bugPathToLoc(targetLoc);
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
                    rc.setIndicatorLine(rc.getLocation(), targetLoc, 255, 0, 255);
                } else {
                    battleStance = CombatStrategy.getBattleStance(target);
                    rc.setIndicatorDot(battleStance, 255, 255, 255);
                    if(rc.canMove(battleStance) && !rc.hasMoved()){
                        rc.move(battleStance);
                    }
                    target = CombatStrategy.getShootingTarget();
                    if(target != null) {
                        targetLoc = target.getLocation();
                        if(prevLoc != null) {
                            nextLoc = targetLoc.add(prevLoc.directionTo(targetLoc), prevLoc.distanceTo(targetLoc));
                            prevLoc = targetLoc;
                            if(CombatStrategy.shotWillHit(rc.getLocation(), nextLoc)){
                                targetLoc = nextLoc;
                            }
                        }
                        float dist = rc.getLocation().distanceTo(targetLoc);
                        if (rc.canFirePentadShot() && dist < 6 && rc.getTeamBullets() > 20 && CombatStrategy.shouldMulti(rc.getLocation(), targetLoc, 5)) {
                            rc.firePentadShot(rc.getLocation().directionTo(targetLoc));
                        }
                        if (rc.canFireTriadShot() && dist < 8 && CombatStrategy.shouldMulti(rc.getLocation(), targetLoc, 3)) {//i don't think 8's a useful number, but has to be >6 because tanks
                            rc.fireTriadShot(rc.getLocation().directionTo(targetLoc));
                        }
                        if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(rc.getLocation().directionTo(targetLoc));
                        }
                    } else {
                        targetLoc = RobotPlayer.INVALID_LOCATION;
                    }
                    rc.setIndicatorLine(rc.getLocation(), targetLoc, 255, 0, 0);

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
