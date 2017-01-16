package scoutrushV3;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
	final static int num_slices = 10;
	final static int num_gardener_slices = 36;
    static RobotController rc;
	static Team FRIEND;
	static Team ENEMY;
	static Team NEUTRAL;
	static Direction forward;
	static Direction backward;
	static Direction left;
	static Direction right;
	static Direction absolute_up;
	static Direction absolute_left;
	static Direction absolute_right;
	static Direction absolute_down;
	static MapLocation invalid_location;
	static MapLocation[] our_archons;
	static MapLocation[] their_archons;
	static float[] potential_angles;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Initialize important variables
        RobotPlayer.rc = rc;
		RobotPlayer.FRIEND = rc.getTeam();
		RobotPlayer.ENEMY = rc.getTeam().opponent();
		RobotPlayer.NEUTRAL = Team.NEUTRAL;
		RobotPlayer.our_archons = rc.getInitialArchonLocations(FRIEND);
		RobotPlayer.their_archons = rc.getInitialArchonLocations(ENEMY);
		RobotPlayer.forward = new Direction(our_archons[0], their_archons[0]);
		RobotPlayer.left = forward.rotateLeftDegrees(90);
		RobotPlayer.backward = left.rotateLeftDegrees(90);
		RobotPlayer.right = backward.rotateLeftDegrees(90);
		RobotPlayer.absolute_up = new Direction((float)(float)Math.PI/2);
		RobotPlayer.absolute_left = absolute_up.rotateLeftDegrees(90);
		RobotPlayer.absolute_down = absolute_left.rotateLeftDegrees(90);
		RobotPlayer.absolute_right = absolute_down.rotateLeftDegrees(90);
		RobotPlayer.invalid_location = new MapLocation(0, 0);
		RobotPlayer.potential_angles = new float[num_slices];
		for(int i = 0; i < num_slices; i++){
			potential_angles[i] = i * 2 * (float)Math.PI / num_slices;
		}
		
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
				rc.broadcast(900, rc.readBroadcast(900) + 1);
            	Archon.runArchon(rc);
                break;
            case GARDENER:
				rc.broadcast(901, rc.readBroadcast(901) + 1);
                Gardener.runGardener(rc);
                break;
            case SOLDIER:
				rc.broadcast(902, rc.readBroadcast(902) + 1);
                Scout.runScout(rc);
                break;
            /*case LUMBERJACK:
            	rc.broadcast(903, rc.readBroadcast(903) + 1);
                Lumberjack.runLumberjack(rc);
                break;*/
			case SCOUT:
				rc.broadcast(904, rc.readBroadcast(904) + 1);
				Scout.runScout(rc);
				break;
        }
	}
	
	static void checkForStockpile() throws GameActionException{
		try{
			if(rc.getRobotCount() > 40 || rc.getRoundNum() > 2800 || rc.getTeamBullets() > 10000 - 10 * rc.getTeamVictoryPoints()){
				rc.donate(rc.getTeamBullets() - (rc.getTeamBullets() % 10));
			}
		} catch(Exception e){
			System.out.println("checkForStockpile() error");
			e.printStackTrace();
		}
	}
	
	static boolean isValidLoc(MapLocation loc){
		if(loc.x == invalid_location.x && loc.y == invalid_location.y){
			return false;
		}
		return true;
	}
	
	static void findShakableTrees() throws GameActionException{
		try{
			//System.out.println("Finding shakable trees");
			TreeInfo[] trees = rc.senseNearbyTrees(-1, NEUTRAL);
			if(trees.length == 0){
				//System.out.println("No shakable trees nearby :(");
				return;
			}
			else{
				TreeInfo best = trees[0];
				for(int i = 0; i < trees.length; i++){
					if(rc.canShake(trees[i].getLocation())){
						if(trees[i].getContainedBullets() > best.getContainedBullets() || !rc.canShake(best.getLocation())){
							best = trees[i];
						}
					}
				}
				if(rc.canShake(best.getLocation())){
					//System.out.println("I am at " + rc.getLocation());
					//System.out.println("Shaking tree at " + best.getLocation() + " to get " + best.getContainedBullets() + " bullets");
					rc.shake(best.getLocation());
				}
			}
		} catch(Exception e){
			System.out.println("findShakableTrees() error");
			e.printStackTrace();
		}
	}
	
	static void updateSurroundings() throws GameActionException{
		// TODO: Make more efficient, uses too many bytecodes		
		
		try{
			RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY);
			ArrayList<Integer> empty_broadcasts = new ArrayList<Integer>();
			ArrayList<Integer> to_overwrite = new ArrayList<Integer>();
			
			//System.out.println("Before reading broadcasts: " + Clock.getBytecodeNum());
			
			for(int i = 0; i < 200; i++){
				/*if(i == 100){
					System.out.println("Before reading broadcast: " + Clock.getBytecodeNum());
				}*/
				int data = rc.readBroadcast(i);
				/*if(i == 100){
					System.out.println("After reading broadcast: " + Clock.getBytecodeNum());
				}*/
				MapLocation prev_sighting = new MapLocation(data / 1000, data % 1000);
				/*if(i == 100){
					System.out.println("Before sensing location: " + Clock.getBytecodeNum());
				}*/
				if(rc.canSenseLocation(prev_sighting)){
					to_overwrite.add(i);
				}
				/*if(i == 100){
					System.out.println("After sensing location: " + Clock.getBytecodeNum());
				}*/
				if(data == 0){
					empty_broadcasts.add(i);
				}
				/*if(i == 100){
					System.out.println("After adding to empty_broadcasts: " + Clock.getBytecodeNum());
				}*/
			}
			
			//System.out.println("After reading broadcasts: " + Clock.getBytecodeNum());
			
			int ptr = 0;
			
			//System.out.println("Before overwriting previously seen locations: " + Clock.getBytecodeNum());
			
			while(ptr < enemies.length && ptr < to_overwrite.size()){
				int encode = (int)enemies[ptr].getLocation().x * 1000 + (int)enemies[ptr].getLocation().y;
				rc.broadcast(to_overwrite.get(ptr), encode);
				ptr++;
			}
			
			//System.out.println("After overwriting previously seen locations: " + Clock.getBytecodeNum());
			
			while(ptr < to_overwrite.size()){
				// runs if less enemies than stuff to overwrite
				rc.broadcast(to_overwrite.get(ptr), 0);
				ptr++;
			}
			while(ptr < enemies.length && ptr < empty_broadcasts.size()){
				// runs if more enemies than stuff to overwrite
				int encode = (int)enemies[ptr].getLocation().x * 1000 + (int)enemies[ptr].getLocation().y;
				rc.broadcast(empty_broadcasts.get(ptr), encode);
				ptr++;
			}
			while(ptr < enemies.length){
				// runs if more enemies than there are empty locations
				// TODO, low priority
			}
		} catch(Exception e){
			System.out.println("updateSurroundings() error");
			e.printStackTrace();
		}
	}
	
	static MapLocation getClosestTarget() throws GameActionException{
		try{
			MapLocation current_closest = invalid_location;
			for(int i = 0; i < 200; i++){
				int data = rc.readBroadcast(i);
				MapLocation prev_sighting = new MapLocation(data / 1000, data % 1000);
				if(rc.getLocation().distanceTo(prev_sighting) < rc.getLocation().distanceTo(current_closest)){
					current_closest = prev_sighting;
				}
			}
			return current_closest;
		} catch(Exception e){
			System.out.println("getClosestTarget() error");
			e.printStackTrace();
			return invalid_location;
		}
	}
	
	static MapLocation getBestShootingLocation(RobotInfo target_robot) throws GameActionException{
		try{
			//System.out.println("Calculating best shooting location... " + Clock.getBytecodeNum());
			MapLocation target = target_robot.getLocation();
			RobotInfo[] robots = rc.senseNearbyRobots();
			TreeInfo[] trees = rc.senseNearbyTrees();
			BulletInfo[] bullets = rc.senseNearbyBullets();
			/*boolean needs_to_move = false;
			Direction vect_to_me = new Direction(target, rc.getLocation());
			float cur_angle = (float)absolute_right.radiansBetween(vect_to_me);*/
			
			float look_distance = 0;
			if(rc.getType() == RobotType.SCOUT){
				if(target_robot.getType() == RobotType.GARDENER){
					look_distance = (float)2.05;
				}
				else if(target_robot.getType() == RobotType.ARCHON){
					look_distance = (float)3.05;
				}
				else if(target_robot.getType() == RobotType.LUMBERJACK){
					look_distance = (float) 4.05;
				}
				else if(target_robot.getType() == RobotType.SOLDIER || target_robot.getType() == RobotType.TANK){
					look_distance = (float) 5.05;
				}
				else{
					look_distance = getSightRadius();
				}
			}
			else{
				look_distance = (float) 3.05;
			}
			
			boolean[] clear_angles = new boolean[num_slices];
			for(int i = 0; i < num_slices; i++){
				clear_angles[i] = true;
			}
			
			//System.out.println("Initialized clear angles... " + Clock.getBytecodeNum());
			
			for(int i = 0; i < robots.length; i++){
				if(target.distanceTo(robots[i].getLocation()) > look_distance){
					continue;
				}
				if(target.distanceTo(robots[i].getLocation()) < 0.5){
					continue;
				}
				//System.out.println("Finding clear angles around robot " + i + "... " + Clock.getBytecodeNum());
				Direction vect = new Direction(target, robots[i].getLocation());
				float dist = (float)target.distanceTo(robots[i].getLocation());
				float spread = (float)Math.asin(robots[i].getRadius()/dist);
				if(spread < 0){
					spread = 2 * (float) Math.PI + spread;
				}
				float center_angle = (float)absolute_right.degreesBetween(vect) * (float)Math.PI / 180;
				if(center_angle < 0){
					center_angle = 2 * (float) Math.PI + center_angle;
				}
				//System.out.println("Calculated angles around robot " + i + ", updating potential_angles... " + Clock.getBytecodeNum());
				for(int j = 0; j < num_slices; j++){
					//System.out.println("Updating angle " + j + "... " + Clock.getBytecodeNum());
					if(center_angle - spread - 0.1 < potential_angles[j] && potential_angles[j] < center_angle + spread + 0.1){
						clear_angles[j] = false;
					}
					/*if(center_angle - spread < cur_angle && cur_angle < center_angle + spread){
						needs_to_move = true;
					}*/
				}
			}
			
			//System.out.println("Calculated angles not blocked by robots... " + Clock.getBytecodeNum());
			
			for(int i = 0; i < trees.length; i++){
				if(target.distanceTo(trees[i].getLocation()) > look_distance || (target_robot.getType() == RobotType.GARDENER && rc.getType() == RobotType.SCOUT)){
					continue;
				}
				Direction vect = new Direction(target, trees[i].getLocation());
				float dist = (float)target.distanceTo(trees[i].getLocation());
				float spread = (float)Math.asin(trees[i].getRadius()/dist);
				if(spread < 0){
					spread = 2 * (float) Math.PI + spread;
				}
				float center_angle = (float)absolute_right.degreesBetween(vect) * (float)Math.PI / 180;
				if(center_angle < 0){
					center_angle = 2 * (float) Math.PI + center_angle;
				}
				//System.out.println("Target is at " + target + " protected by tree at " + trees[i].getLocation() + " for angles " + center_angle + " " + spread);
				for(int j = 0; j < num_slices; j++){
					if(center_angle - spread - 0.1 < potential_angles[j] && potential_angles[j] < center_angle + spread + 0.1){
						clear_angles[j] = false;
					}
					/*if(center_angle - spread < cur_angle && cur_angle < center_angle + spread){
						needs_to_move = true;
					}*/
				}
			}
			
			/*if(!needs_to_move){
				return rc.getLocation();
			}*/
			
			//System.out.println("Calculated angles not blocked by trees... " + Clock.getBytecodeNum());
			
			ArrayList<MapLocation> potential_loc = new ArrayList<MapLocation>();
			
			for(int i = 0; i < num_slices; i++){
				if(!clear_angles[i]){
					continue;
				}
				Direction potential_dir = absolute_right.rotateLeftRads(potential_angles[i]);
				MapLocation potential_shooting_loc = target.add(potential_dir, look_distance);
				if(rc.canMove(potential_shooting_loc)){
					boolean will_add = true;
					for(int j = 0; j < bullets.length; j++){
						if(willCollideWithLoc(rc.getLocation().add(rc.getLocation().directionTo(potential_shooting_loc), (float)Math.min(2.5, rc.getLocation().distanceTo(potential_shooting_loc))), bullets[j])){
							will_add = false;
						}
					}
					if(will_add){
						potential_loc.add(potential_shooting_loc);
					}
				}
			}
			
			//System.out.println("Calculated potential shooting spots... " + Clock.getBytecodeNum());
			
			if(potential_loc.size() == 0){
				//System.out.println("No shooting location found, " + Clock.getBytecodeNum());
				return invalid_location;
			}
			else{
				MapLocation best = potential_loc.get(0);
				for(int i = 0; i < potential_loc.size(); i++){
					if(rc.getLocation().distanceTo(potential_loc.get(i)) < rc.getLocation().distanceTo(best)){
						best = potential_loc.get(i);
					}
				}
				//System.out.println("Shooting location found! " + Clock.getBytecodeNum());
				return best;
			}
		} catch(Exception e){
			System.out.println("getBestShootingLocation() error");
			e.printStackTrace();
			return invalid_location;
		}
	}
	
	static float getSightRadius(){
		switch(rc.getType()){
			case ARCHON:
				return (float)10.0;
			case GARDENER:
				return (float)7.0;
			case SOLDIER:
				return (float)7.0;
			case TANK:
				return (float)7.0;
			case SCOUT:
				return (float)10.0;
			case LUMBERJACK:
				return (float)7.0;
		}
		return 0;
	}
	
	static boolean willBeHit(Direction dir) throws GameActionException{
		try{
			BulletInfo[] bullets = rc.senseNearbyBullets();
			MapLocation next_location = rc.getLocation().add(dir);
			for(int i = 0; i < bullets.length; i++){
				if(willCollideWithLoc(next_location, bullets[i])){
					return true;
				}
			}
			return false;
		}catch(Exception e){
			System.out.println("willBeHit() error");
			e.printStackTrace();
			return false;
		}
	}
	

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
		int rand_idx = (int)(num_slices * Math.random());
        return absolute_right.rotateLeftRads(potential_angles[rand_idx]);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
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
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

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
    static boolean willCollideWithLoc(MapLocation loc, BulletInfo bullet) {
        MapLocation myLocation = loc;
		
		if(myLocation.distanceTo(bullet.location) >= 6){ 
			return false;
		}

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > (float)Math.PI/2) {
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
