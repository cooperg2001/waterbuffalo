package BuffaloV0;
import battlecode.common.*;

/**
 * Created by Zach on 1/27/2017.
 */
public class CombatStrategy {

    public static MapLocation locateTarget(){
        RobotController rc = RobotPlayer.rc;
        MapLocation[] potentialTargets = RobotPlayer.last_sighting_location;

        //use secondary targets and find which type is the one we want to target
        int priorityType = 0;
        for(int i = 1; i < potentialTargets.length; i++) {
            if(!potentialTargets[i].equals(RobotPlayer.INVALID_LOCATION)
                    && rc.canMove(potentialTargets[i])
                    && RobotPlayer.getPriority(rc.getType(), RobotPlayer.intToType(i)) < RobotPlayer.getPriority(rc.getType(), RobotPlayer.intToType(priorityType))) {
                priorityType = i;
            }
        }
        return potentialTargets[priorityType];
    }

    public static RobotInfo getShootingTarget() throws GameActionException{
        try {
            for (int i = 0; i < RobotPlayer.enemies.length; i++) {
                for (int j = 0; j < RobotPlayer.enemies[i].length; j++) {
                    if (RobotPlayer.shotWillHit(RobotPlayer.rc.getLocation(), RobotPlayer.enemies[i][j])) {
                        return RobotPlayer.enemies[i][j];
                    }
                }
            }
        } catch(Exception e) {
            System.out.println("getShootingTarget() error");
            e.printStackTrace();
        }
        return null;
    }

    public static MapLocation getBattleStance(RobotInfo target) throws GameActionException{
        try {
            final int SPRING_CONSTANT = 1;
            float[] force = {0f, 0f}; //[x, y]
            float optimalDistance;
            float displacement;
            float distance;
            MapLocation ours = RobotPlayer.rc.getLocation();
            Direction dir;

            for (RobotInfo robot : RobotPlayer.robots) {
                if (RobotPlayer.shotWillHit(RobotPlayer.rc.getLocation(), robot)) {
                    optimalDistance = RobotPlayer.getOptimalDist(RobotPlayer.rc.getType(), robot.getType());
                    distance = ours.distanceTo(robot.getLocation());
                    displacement = distance - optimalDistance;
                    dir = ours.directionTo(robot.getLocation());
                    if(displacement < 0 || robot.equals(target)) {
                        force[0] += SPRING_CONSTANT * displacement * Math.cos(dir.radians);
                        force[1] += SPRING_CONSTANT * displacement * Math.sin(dir.radians);
                    }
                }
            }
            return new MapLocation(ours.x + force[0], ours.y + force[1]);
        } catch(Exception e){
            System.out.println("getBattleStance() error");
            e.printStackTrace();;
        }
        return null;
    }
}
