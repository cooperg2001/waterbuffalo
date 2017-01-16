package turtleV1;

import battlecode.common.*;

/**
 * Created by Zach on 1/12/2017.
 */
public class Archon {

    static void runArchon(RobotController rc) throws GameActionException {

        Direction rand = RobotPlayer.randomDirection();

        while (true) {

            try {

                if (rc.getTeamBullets() > 200) {
                    rc.donate(rc.getTeamBullets() - (rc.getTeamBullets() % 10));
                }

                for (int i = 0; i < RobotPlayer.num_slices; i++) {
                    Direction dir = RobotPlayer.absolute_right.rotateLeftRads(RobotPlayer.potential_angles[i]);
                    if (rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                        break;
                    }
                }

                int trials = 0;
                while (!rc.canMove(rand) && trials < 10) {
                    rand = RobotPlayer.randomDirection();
                    trials++;
                }
                if (rc.canMove(rand)) {
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
