package asimplefarmer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
	
	private static Direction left;
	private static Direction right;
	private static Direction up;
	private static Direction down;
	private static Direction forward;
	private static Direction back;
	private static Direction upish;
	private static Direction downish;
	
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
		
		MapLocation[] ours = rc.getInitialArchonLocations(rc.getTeam());
		MapLocation[] theirs = rc.getInitialArchonLocations(rc.getTeam().opponent());
		boolean isTeamA;
		if(ours[0].x < theirs[0].x){
			isTeamA = true;
		}
		else{
			isTeamA = false;
		}
				
		left = new Direction((float)Math.PI);
		right = new Direction((float)0);
		up = new Direction((float)Math.PI/2);
		down = new Direction((float)Math.PI/2*3);
		forward = new Direction(ours[0], theirs[0]);
		back = new Direction(theirs[0], ours[0]);
		up = forward.rotateLeftDegrees(90);
		down = forward.rotateRightDegrees(90);
		
		Team enemy = rc.getTeam().opponent();
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		MapLocation INVALID = new MapLocation(-1, -1);
		
		for(int i = 0; i < robots.length; i++){
			addEnemy(robots[i]);
		}
		if(robots.length == 0){
			MapLocation target = getEnemy();
			while(target.x != INVALID.x && rc.getLocation().isWithinDistance(target, 7)){
				removeEnemy();
				target = getEnemy();
			}
		}
		
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				
		if (trees.length > 0){
			for(int i = 0; i < trees.length; i++){
				if(rc.canShake(trees[i].getLocation())){
					rc.shake(trees[i].getLocation());
				}
			}
		}
		
		/*for(int i = 0; i < 200; i++){
			System.out.println(rc.getRoundNum() + " " + i + " " + rc.readBroadcast(i));
		}*/
		
		
		switch (rc.getType()) {
			case ARCHON:
				runArchon();
				break;
			case GARDENER:
				runGardener();
				break;
			case SOLDIER:
				runSoldier();
				break;
			case TANK:
				runSoldier();
			case SCOUT:
				runScout();
				break;
			/*case LUMBERJACK:
				runLumberjack();
				break;*/
		}
		
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
		
		int offset = 0;
		int hired = 0;
		boolean canMoveUpish = true;
		boolean danceright = true;
		
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				if(rc.getRoundNum() == 2998){
					rc.donate(rc.getTeamBullets());
				}
				if(rc.getRoundNum() > 2900){
					tryMove(randomDirection(),1);
					Clock.yield();
					continue;
				}
				
				if(rc.getTeamBullets() >= 10000){
					rc.donate(10000);
				}
				
				if(canMoveUpish && rc.getRoundNum() < 20){
					canMoveUpish = tryMove(forward, 1);
					Clock.yield();
				}
				else{
					if(offset < 5 && danceright){
						tryMove(back, 1);
						offset += 1;
					}
					else{
						if(danceright){
							tryMove(back, 5);
							danceright = false;
						}
						else{
							tryMove(forward,5);
							danceright = true;
						}
					}

					// Always generate a gardener if possible
					if(offset == 5 && rc.canHireGardener(forward) && danceright) {
						rc.hireGardener(forward);
						hired++;
						if(hired == 2){
							hired = 0;
							offset = 0;
						}
						rc.broadcast(300, rc.readBroadcast(300) + 1); // Number of gardeners
					}

					// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
					Clock.yield();
				}

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
		
		boolean isRightMost = false;
		
		if(rc.readBroadcast(300) < 2 * rc.getInitialArchonLocations(rc.getTeam()).length){
			isRightMost = true;
		}

        // The code you want your robot to perform every round should be in this loop
		
		boolean going_up = true;
		boolean make_scout = true;
		
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				if(rc.getRoundNum() == 2998){
					rc.donate(rc.getTeamBullets());
				}
				if(rc.getRoundNum() > 2900){
					tryMove(randomDirection(),1);
					Clock.yield();
					continue;
				}
				
				if(going_up){
					if(!rc.canMove(up)){
						going_up = false;
					}
					else{
						rc.move(up);
					}
				}
				else{
					if(!rc.canMove(down)){
						going_up = true;
					}
					else{
						rc.move(down);
					}
				}
				
				if(rc.getRoundNum() > 500){
					if(rc.canBuildRobot(RobotType.SOLDIER, forward)){
						rc.buildRobot(RobotType.SOLDIER,forward);
						rc.broadcast(301, rc.readBroadcast(301) + 1);
					}
				}
				if(rc.getRoundNum() > 60 && rc.getRoundNum() < 1000 && (rc.getRoundNum() > 500 || rc.readBroadcast(301) < 5 * rc.readBroadcast(300))){
					if(rc.canBuildRobot(RobotType.SCOUT, forward)){
						rc.buildRobot(RobotType.SCOUT, forward);
						rc.broadcast(301, rc.readBroadcast(301) + 1);
					}
				}
				
				if(rc.canPlantTree(back)){
					rc.plantTree(back);
				}
				else{
					if(rc.canBuildRobot(RobotType.SOLDIER,forward) && rc.getRoundNum() > 500){
						rc.buildRobot(RobotType.SOLDIER,forward);
						rc.broadcast(301, rc.readBroadcast(301) + 1);
					}
				}
				
				TreeInfo[] tree_list = rc.senseNearbyTrees((float)3.0, rc.getTeam());
				if(tree_list.length > 0){
					TreeInfo best = tree_list[0];
					for(int i = 0; i < tree_list.length; i++){
						if(tree_list[i].getHealth() < best.getHealth() && rc.canWater(tree_list[i].getLocation())){
							best = tree_list[i];
						}
					}
					if(rc.canWater(best.getLocation())){
						rc.water(best.getLocation());
					}
				}
				
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
		
		MapLocation INVALID = new MapLocation(-1, -1);
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				if(rc.getRoundNum() == 2998){
					rc.donate(rc.getTeamBullets());
				}
				if(rc.getRoundNum() > 2900){
					tryMove(randomDirection(),1);
					Clock.yield();
					continue;
				}
				
				Direction move_target = forward;
				
                MapLocation myLocation = rc.getLocation();
				MapLocation target = getEnemy();
				//System.out.println(target.x + " " + target.y);

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(3, enemy);
				
                // If there are some...
                if (robots.length > 0) {
					RobotInfo priority = robots[0];
					for(int i = 0; i < robots.length; i++){
						if(robots[i].type == RobotType.SOLDIER || (priority.type != RobotType.SOLDIER && robots[i].type == RobotType.TANK)){
							priority = robots[i];
						}
					}
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
						if(rc.getTeamBullets() >= 500){
							rc.firePentadShot(rc.getLocation().directionTo(priority.location));
						}
                        rc.fireSingleShot(rc.getLocation().directionTo(priority.location));
                    }
                }else{
					if(rc.getLocation().isWithinDistance(target, 7)){
						removeEnemy();
					}
				}
				
				TreeInfo[] trees = rc.senseNearbyTrees(3, Team.NEUTRAL);
				
				if (trees.length > 0){
					for(int i = 0; i < trees.length; i++){
						if(rc.canShake(trees[i].getLocation())){
							rc.shake(trees[i].getLocation());
						}
						if(rc.getType() == RobotType.TANK){
							if(!rc.hasMoved() && rc.canMove(rc.getLocation().directionTo(trees[i].location))){
								rc.move(rc.getLocation().directionTo(trees[i].location));
							}
						}
						if(rc.canFireSingleShot() && rc.getTeamBullets() >= 300){
							rc.fireSingleShot(rc.getLocation().directionTo(trees[i].location));
						}
					}
				}
				
				
				if(target.x != INVALID.x){
					move_target = rc.getLocation().directionTo(target);
					tryMove(move_target, 1);
				}
				if(rc.getRoundNum() % 20 == 0){
					tryMove(move_target, 1);
				}

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
	
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		Team enemy = rc.getTeam().opponent();
		Direction rand = randomDirection();
		Direction move_target;
		MapLocation INVALID = new MapLocation(-1, -1);
		
		while(true){
			try{
				if(rc.getRoundNum() == 2998){
					rc.donate(rc.getTeamBullets());
				}
				if(rc.getRoundNum() > 2900){
					tryMove(randomDirection(),1);
					Clock.yield();
					continue;
				}
				
				MapLocation target = getEnemy();
				
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
				
				if(robots.length > 0){
					for(int i = 0; i < robots.length; i++){
						addEnemy(robots[i]);
						if(rc.canFireSingleShot() && rc.getLocation().isWithinDistance(robots[i].getLocation(),5)){
							tryMove(randomDirection(),3);
							rc.fireSingleShot(rc.getLocation().directionTo(robots[i].getLocation()));
						}
					}
				}else{
					//System.out.println(rc.readBroadcast(101));
					//System.out.println(rc.getLocation().x + " " + rc.getLocation().y + " " + target.x + " " + target.y + " " + rc.getLocation().distanceTo(target));
					if(rc.getLocation().distanceTo(target) < 10){
						System.out.println("Removing target...");
						removeEnemy();
					}
				}
				
				if(rc.getRoundNum()%10 == 0){
					rand = randomDirection();
				}
								
				if(target.x != INVALID.x && rc.getLocation().distanceTo(target) > 2.5){
					move_target = rc.getLocation().directionTo(target);
				}
				else{
					move_target = rand;
				}
				tryMove(move_target, (float)2.5);
				if(!rc.hasMoved()){
					rand = randomDirection();
				}

				Clock.yield();
			} catch(Exception e){
				System.out.println("Scout exception");
				e.printStackTrace();
			}
		}
	}
	
	static void addEnemy(RobotInfo enemy) throws GameActionException{
		try{
			int encode = (int)enemy.getLocation().x * 1000 + (int)enemy.getLocation().y;
			int ptr = rc.readBroadcast(0);
			int otherptr = rc.readBroadcast(101);
			if(ptr == 0){
				ptr = 1;
			}
			if(otherptr == 0){
				otherptr = 102;
			}
			
			if(ptr == 101){
				while(ptr > 1 && otherptr < 201){
					rc.broadcast(otherptr, rc.readBroadcast(ptr - 1));
					otherptr++;
					ptr--;
				}
			}
			
			rc.broadcast(101, otherptr);
				
			if(ptr == 101){
				return;
			}
			else{
				rc.broadcast(ptr, encode);
				ptr++;
				rc.broadcast(0, ptr);
			}
		} catch(Exception e){
			System.out.println("addEnemy exception");
			e.printStackTrace();
		}
	}
	
	static MapLocation getEnemy() throws GameActionException{
		try{
			int ptr = rc.readBroadcast(0);
			int otherptr = rc.readBroadcast(101);
			//System.out.println("problem not here");
			//System.out.println(ptr);
			//System.out.println(otherptr);
			if(ptr == 0){
				ptr = 1;
			}
			if(otherptr == 0){
				otherptr = 102;
			}
			
			if(otherptr == 102){
				while(ptr > 1 && otherptr < 201){
					rc.broadcast(otherptr, rc.readBroadcast(ptr - 1));
					otherptr++;
					ptr--;
				}
			}
			
			rc.broadcast(101, otherptr);
			
			if(otherptr == 102){
				return new MapLocation(-1, -1);
			}
			
			int encodedloc = rc.readBroadcast(otherptr - 1);
			return new MapLocation(encodedloc / 1000, encodedloc % 1000);
		} catch(Exception e){
			System.out.println("getEnemy exception");
			/*for(int i = 0; i < 1000; i++){
				System.out.println(i + ": " + rc.readBroadcast(i));
			}
			System.exit(0);*/
			e.printStackTrace();
			return new MapLocation(-1, -1);
		}
	}
	
	static void removeEnemy() throws GameActionException{
		try{
			int ptr = rc.readBroadcast(0);
			int otherptr = rc.readBroadcast(101);
			if(ptr == 0){
				ptr = 1;
			}
			if(otherptr == 0){
				otherptr = 102;
			}
			
			if(otherptr == 102){
				return;
			}
			else{
				otherptr--;
				rc.broadcast(101, otherptr);
			}
		} catch(Exception e){
			System.out.println("removeEnemy exception");
			e.printStackTrace();
		}
	}
	
	static boolean inCombat() throws GameActionException{
		try{
			MapLocation INVALID = new MapLocation(-1, -1);
			if(getEnemy().x != INVALID.x){
				return true;
			}
			return false;
		} catch(Exception e){
			System.out.println("inCombat exception");
			e.printStackTrace();
			return false;
		}
	}
	
    /*static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float dist) throws GameActionException {
        return tryMove(dir,20,3,dist);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide, float dist) throws GameActionException {
		
		if(rc.hasMoved()){
			return false;
		}
		
        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
