package soldierrush;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	static Team FRIEND;
	static Team ENEMY;
	static Team NEUTRAL;
	static Direction forward; // Heuristically speaking, a direction towards the enemy
	static Direction backward;
	static Direction left;
	static Direction right;
	static Direction absolute_up;
	static Direction absolute_left;
	static Direction absolute_right;
	static Direction absolute_down;
	static MapLocation INVALID_LOCATION;
	static MapLocation[] our_archons;
	static MapLocation[] their_archons;
	static MapLocation center;
	static float[] potential_angles;
	final static int num_angles = 24;
	final static int num_steps = 5;
	static RobotInfo[] robots;
	static RobotInfo[][] enemies;
	static TreeInfo[] trees;
	static TreeInfo[] neutral_trees;
	static TreeInfo[] their_trees;
	static TreeInfo[] our_trees;
	static BulletInfo[] bullets;
	static MapLocation last_sighting_location[];

	/**
	 * BROADCAST DIRECTORY
	 * 500 - last observed location of ENEMY archon target
	 * 501 - last observed location of ENEMY gardener target
	 * 502 - last observed location of ENEMY lumberjack target
	 * 503 - last observed location of ENEMY scout target
	 * 504 - last observed location of ENEMY soldier target
	 * 505 - last observed location of ENEMY tank target
	 *
	 * 900 - estimated # of FRIEND archons
	 * 901 - estimated # of FRIEND gardeners
	 * 902 - estimated # of FRIEND lumberjacks
	 * 903 - estimated # of FRIEND scouts
	 * 904 - estimated # of FRIEND soldiers
	 * 905 - estimated # of FRIEND tanks
	 */

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

		// Initialize important variables
		RobotPlayer.rc = rc; // Current robot controller
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
		RobotPlayer.INVALID_LOCATION = new MapLocation(999, 999);
		RobotPlayer.last_sighting_location = new MapLocation[6];
		RobotPlayer.enemies = new RobotInfo[6][1];
		float centerx = 0;
		float centery = 0;
		for(int i = 0; i < our_archons.length; i++){
			centerx += our_archons[i].x;
			centery += our_archons[i].y;
			centerx += their_archons[i].x;
			centery += their_archons[i].y;
		}
		RobotPlayer.potential_angles = new float[num_angles];
		for(int i = 0; i < num_angles; i++){
			potential_angles[i] = i * 2 * (float)Math.PI / num_angles;
		}
		center = new MapLocation(centerx / our_archons.length, centery / our_archons.length);

		//increment broadcast for storing # of each type of robot in our army
		/*rc.broadcast(900 + typeToInt(rc.getType()), rc.readBroadcast(900 + typeToInt(rc.getType())) + 1);
		System.out.println("I'm a " + rc.getType() + "!");
		System.out.println("Have "  + rc.readBroadcast(903) + " scouts and " + rc.readBroadcast(904) + " soldiers.");*/

		// Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
            	Archon.runArchon(rc);
                break;
            case GARDENER:
                Gardener.runGardener(rc);
                break;
            case LUMBERJACK:
                Lumberjack.runLumberjack(rc);
                break;
			case SCOUT:
				CombatUnit.runCombatUnit(rc);
				break;
			case SOLDIER:
				CombatUnit.runCombatUnit(rc);
				break;
			case TANK:
				CombatUnit.runCombatUnit(rc);
				break;
		}
	}

	/**
	 *  HELPER FUNCTIONS LIST --
	 *  shotWillHit
	 *  get_best_location
	 *  get_priority_target
	 *  updateEnemiesAndBroadcast
	 *  getBestShootingLocation
	 *  dodgeBullets
	 *  intToType
	 *  typeToInt
	 *  getOptimalDist
	 *  getPriority
	 *  CircleIntersectsLine
	 *  findShakableTrees
	 *  checkForStockpile
	 *  randomDirection
	 **/


	/**
	 * shotWillHit
	 *
	 * @param loc (MapLocation) Location of rc
	 * @param target (RobotInfo) robot to shoot
	 * @return (boolean) whether or not a shot you shoot at an enemy will actually hit an enemy
	 * 					-- in particular, makes sure there aren't any trees in the way
	 * @throws GameActionException
	 */

	public static boolean shotWillHit(MapLocation loc, RobotInfo target) throws GameActionException{
		MapLocation bullet_hit_location = target.getLocation().add(target.getLocation().directionTo(loc), target.getType().bodyRadius);
		MapLocation bullet_start_location = loc.add(loc.directionTo(target.getLocation()), 1);

		for(int i = 0; i < robots.length; i++){
			if(bullet_hit_location.distanceTo(robots[i].getLocation()) > bullet_hit_location.distanceTo(loc) - 0.01){
				continue;
			}
			if(bullet_hit_location.distanceTo(robots[i].getLocation()) < 1.01){
				continue;
			}
			if(bullet_start_location.distanceTo(robots[i].getLocation()) < 1.01){
				continue;
			}
			if(CircleIntersectsLine(robots[i].getLocation(), robots[i].getType().bodyRadius, loc, bullet_hit_location)){
				return false;
			}
		}

		for(int i = 0; i < trees.length; i++){
			if(bullet_hit_location.distanceTo(trees[i].getLocation()) > bullet_hit_location.distanceTo(loc) - 0.01){
				continue;
			}
			if(bullet_hit_location.distanceTo(trees[i].getLocation()) < 1){
				continue;
			}
			if(bullet_start_location.distanceTo(trees[i].getLocation()) < 1.01){
				continue;
			}
			if(CircleIntersectsLine(trees[i].getLocation(), trees[i].getRadius(), loc, bullet_hit_location)){
				return false;
			}
		}
		return true;
	}


	/**
	 *
	 * @return Best location according to heuristic
	 * 			-- If you're a scout and it's early, rush their archons
	 * 			-- Otherwise, if you're a combat unit, rush highest priority enemy
	 * 			-- Otherwise, dodge bullets if it's not early
	 * 			-- Otherwise, move towards the last seen enemy of highest priority
	 * @throws GameActionException
	 */

	public static MapLocation get_best_location() throws GameActionException {
		MapLocation best_location;

		// If you're a scout and it's early, rush their archons
		if (rc.getType() == RobotType.SCOUT
				&& rc.getRoundNum() < 100
				&& rc.getLocation().distanceTo(RobotPlayer.their_archons[0]) > 5){
			best_location = RobotPlayer.their_archons[0];
			return best_location;
		}

		//System.out.println("Finding best location... " + Clock.getBytecodeNum());

		// Find # of total enemies we sensed
		int totalEnemies = 0;
		for (int i = 0; i < enemies.length; i++) {
			totalEnemies += enemies[i].length;
		}
		// Disregard archons
		if(rc.getRoundNum() < 300){
			totalEnemies -= enemies[0].length;
		}
		// If we can see an enemy, move towards the one with the highest priority
		if(totalEnemies > 0 && rc.getType() != RobotType.ARCHON){
			RobotInfo priority_target = get_priority_target();
			best_location = getBestShootingLocation(priority_target);
			if (best_location.x != INVALID_LOCATION.x && rc.canMove(best_location)) {
				//System.out.println("Found a good shooting location, going to " + best_location + " used " + Clock.getBytecodeNum());
				return best_location;
			}
		}

		//System.out.println("Found no good shooting location. Dodging bullets... " + Clock.getBytecodeNum());
		if (rc.getRoundNum() > 100){
			best_location = dodgeBullets();
			if(best_location.x != INVALID_LOCATION.x && rc.canMove(best_location)){
				//System.out.println("Dodged " + bullets.length + " bullets using " + Clock.getBytecodeNum() + " going towards " + best_location);
				return best_location;
			}
		}

		//use secondary targets and find which type is the one we want to target
		int priorityType = 0;
		for(int i = 1; i < last_sighting_location.length; i++) {
			if(last_sighting_location[i].x != INVALID_LOCATION.x
					&& rc.canMove(last_sighting_location[i])
					&& getPriority(rc.getType(), intToType(i)) < getPriority(rc.getType(), intToType(priorityType))) {
				priorityType = i;
			}
		}
		//set the best location and one last check to see if it's dandy
		best_location = last_sighting_location[priorityType];
		// Disregard archons if you're a scout and it's early
		if(rc.getType() == RobotType.SCOUT && rc.getRoundNum() < 300 && priorityType == 0) {
			best_location = INVALID_LOCATION;
		}
		// If you're not an archon...
		if(best_location != INVALID_LOCATION && rc.canMove(best_location) && rc.getType() != RobotType.ARCHON) {
			//System.out.println("Found no way to dodge " + bullets.length + " bullets. Heading towards secondary target... " + best_location + " used " + Clock.getBytecodeNum());
			return best_location;
		}

		//nothing worked, move randomly
		//System.out.println("No secondary target. Found no good movement location, move randomly. " + Clock.getBytecodeNum());
		return INVALID_LOCATION;
	}

	/**
	 * @return (RobotInfo) the robot in RobotPlayer.enemies with the highest priority relative to rc.
	 * 						-- If multiple robots have highest priority, chooses the first one.
	 * @throws GameActionException
	 */

	public static RobotInfo get_priority_target() throws GameActionException{
		RobotInfo priority_target = null; //this strange initialization is needed since enemies[0], for example, might not have length > 0, so we can't just set it to [0][0]
		for(int i = 0; i < enemies.length; i++) {
			if(enemies[i].length > 0) {
				priority_target = enemies[i][0];
				break;
			}
		}
		if(priority_target == null) {
			return null; //something REALLY went wrong if this happens
			// Most likely, have not updated enemies yet.
		}
		for(int i = 0; i < enemies.length; i++){
			for(int j = 0; j < enemies[i].length; j++){
				if (getPriority(rc.getType(), enemies[i][j].getType()) < getPriority(rc.getType(), priority_target.getType())) {
					priority_target = enemies[i][j];
				} else if (getPriority(rc.getType(), enemies[i][j].getType()) == getPriority(rc.getType(), priority_target.getType()) && enemies[i][j].getHealth() < priority_target.getHealth()) {
					priority_target = enemies[i][j];
				}
			}
		}
		return priority_target;
	}

	/**
	 * Overwrites broadcast [500,505] with closest enemies of each type to robot
	 * Sets RobotPlayer.enemies to be a matrix of all enemies seen this by this robot this tick
	 * @throws GameActionException
	 */

	public static void updateEnemiesAndBroadcast() throws GameActionException {
		int[] last_sighting_location_encoded = {rc.readBroadcast(500),
				rc.readBroadcast(501),
				rc.readBroadcast(502),
				rc.readBroadcast(503),
				rc.readBroadcast(504),
				rc.readBroadcast(505)};
		for(int i = 0; i < last_sighting_location_encoded.length; i++) {
			last_sighting_location[i] = new MapLocation(last_sighting_location_encoded[i] / 1000, last_sighting_location_encoded[i] % 1000);
		}

		//This section makes the matrix of enemies sorted by types
		//DISCLAIMER: There may be a significantly more elegant way to do this but it's like 1am and it's how I did it
		RobotInfo[] enemiesRaw = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
		enemies = new RobotInfo[6][0]; //matrix of enemies [type number][id]
		int[] enemiesTypes = new int[enemiesRaw.length];
		int[] numEachType = new int[6];
		for(int i = 0; i < enemiesRaw.length; i++) {
			enemiesTypes[i] = typeToInt(enemiesRaw[i].getType()); //array of each enemy's type
		}
		for(int i = 0; i < enemiesRaw.length; i++) {
			numEachType[enemiesTypes[i]]++; //Count number of each type of enemy
		}
		for(int i = 0; i < 6; i++) { //this sets the length of each type array (eg. how many archons, how many soldiers,...)
			enemies[i] = new RobotInfo[numEachType[i]];
		}
		int counter[] = {0,0,0,0,0,0};
		for(int i = 0; i < enemiesRaw.length; i++) { //this fills the matrix
			enemies[enemiesTypes[i]][counter[enemiesTypes[i]]] = enemiesRaw[i];
			counter[enemiesTypes[i]]++;
		}

		//Now Process enemies matrix by type
		for(int i = 0; i < 6; i++) {
			if(enemies[i].length == 0) {
				if(rc.getLocation().distanceTo(last_sighting_location[i]) < rc.getType().sensorRadius) {
					//Probably dead/gone, so set this to INVALID LOCATION
					rc.broadcast(500 + i, 999999);
				}
			}
			else {
				//Enemy found, update broadcast to first of this type
				rc.broadcast(500 + i, (int)enemies[i][0].getLocation().x * 1000 + (int)enemies[i][0].getLocation().y);
			}
		}
	}

	/**
	 *
	 * @param priority_target (RobotInfo) enemy robot we wish to shoot at
	 * @return (MapLocation) best position to move to so we can shoot at the robot without being in harm's way
	 * @throws GameActionException
	 */

	static MapLocation getBestShootingLocation(RobotInfo priority_target) throws GameActionException{

		// Find distance the robot SHOULD be away from the enemy
		float optimalDist = getOptimalDist(rc.getType(), priority_target.getType());


		for(int i = 0; i < num_angles; i++){
			// If we've used too much bytecode already, just hang
			if(Clock.getBytecodeNum() > 8000){
				return INVALID_LOCATION;
			}

			// Create potential locations optimalDist away from priority_target
			Direction target_to_robot = priority_target.getLocation().directionTo(rc.getLocation());
			Direction potential_dir;
			// Fix potential_dir so that it is in a 180-degree arc centered at target_to_robot
			if(i % 2 == 0){
				potential_dir = target_to_robot.rotateLeftDegrees((int)((i + 1)/2) * 360 / num_angles);
			}
			else{
				potential_dir = target_to_robot.rotateRightDegrees((int)((i + 1)/2) * 360 / num_angles);
			}

			MapLocation potential_shooting_loc = priority_target.getLocation().add(potential_dir, optimalDist);
			MapLocation step_towards_shooting_loc = rc.getLocation().add(rc.getLocation().directionTo(potential_shooting_loc), Math.min(rc.getLocation().distanceTo(potential_shooting_loc), rc.getType().strideRadius));
			MapLocation small_step_towards_shooting_loc = rc.getLocation().add(rc.getLocation().directionTo(potential_shooting_loc), Math.min(rc.getLocation().distanceTo(potential_shooting_loc), (float)rc.getType().bulletSpeed - (float)rc.getType().bodyRadius - (float)0.1));
			/*MapLocation midpoint = priority_target.getLocation().add(potential_dir, optimalDist / 2);
			if(rc.senseNearbyRobots(midpoint, optimalDist / 2 - (float)1.1, null).length > 0){
				continue;
			}
			if(rc.senseNearbyTrees(midpoint, optimalDist / 2 - (float)1.1, null).length > 0){
				continue;
			}*/
			/*if(step_towards_shooting_loc.distanceTo(priority_target.getLocation()) < optimalDist + 1 && !shotWillHit(potential_shooting_loc, priority_target)){
				continue;
			}*/

			boolean will_add_step = true;
			boolean will_add_small_step = true;

			// Make sure we don't hit any bullets if we go there
			for(int j = 0; j < bullets.length; j++){
				if(Clock.getBytecodeNum() > 8000){
					return INVALID_LOCATION;
				}
				if(CircleIntersectsLine(step_towards_shooting_loc, rc.getType().bodyRadius + (float)0.02, bullets[j].getLocation(), bullets[j].getLocation().add(bullets[j].getDir(), bullets[j].getSpeed()))){
					will_add_step = false;
				}
				if(CircleIntersectsLine(small_step_towards_shooting_loc, rc.getType().bodyRadius + (float)0.02, bullets[j].getLocation(), bullets[j].getLocation().add(bullets[j].getDir(), bullets[j].getSpeed()))){
					will_add_small_step = false;
				}
			}

			// Check if moving there will result in being too close to a lumberjack or soldier
			for(int j = 0; j < robots.length; j++){
				if(robots[j].getType() == RobotType.LUMBERJACK || robots[j].getType() == RobotType.SOLDIER){
					if(rc.getType() == RobotType.SCOUT && step_towards_shooting_loc.distanceTo(robots[j].getLocation()) < 4.55){
						will_add_step = false;
					}
				}
				if(robots[j].getType() == RobotType.LUMBERJACK || robots[j].getType() == RobotType.SOLDIER){
					if(rc.getType() == RobotType.SCOUT && step_towards_shooting_loc.distanceTo(robots[j].getLocation()) < 4.55){
						will_add_small_step = false;
					}
				}
			}
			if(will_add_step){
				return step_towards_shooting_loc;
			}
			if(will_add_small_step){
				return small_step_towards_shooting_loc;
			}
		}

		return INVALID_LOCATION;
	}

	/**
	 * @return (Location) a location that is out of the way of each bullet the robot has senced
	 * @throws GameActionException
	 */

	static MapLocation dodgeBullets() throws GameActionException{
		//int orig_bytecodes = Clock.getBytecodeNum();
		float vector_x = (float) 0;
		float vector_y = (float) 0;

		for(int i = 0; i < bullets.length; i++){
			MapLocation new_bullet_location = bullets[i].getLocation().add(bullets[i].getDir(), 2 * bullets[i].getSpeed());
			/*if(!CircleIntersectsLine(rc.getLocation(), rc.getType().bodyRadius, bullets[i].getLocation(), new_bullet_location)){
				continue;
			}*/
			float bullet_vector_x = new_bullet_location.x;
			float bullet_vector_y = new_bullet_location.y;
			float angle = bullets[i].getLocation().directionTo(rc.getLocation()).radiansBetween(bullets[i].getDir());
			if(angle < 0){
				vector_x += bullet_vector_y * (rc.getType().bodyRadius + 0.01) / bullets[i].getSpeed();
				vector_y -= bullet_vector_x * (rc.getType().bodyRadius + 0.01) / bullets[i].getSpeed();
			}
			else{
				vector_x -= bullet_vector_y * (rc.getType().bodyRadius + 0.01) / bullets[i].getSpeed();
				vector_y += bullet_vector_x * (rc.getType().bodyRadius + 0.01) / bullets[i].getSpeed();
			}
		}

		//System.out.println("Dodged " + bullets.length + " bullets using " + (Clock.getBytecodeNum() - orig_bytecodes));

		MapLocation target = new MapLocation(rc.getLocation().x + vector_x, rc.getLocation().y + vector_y);
		if(rc.getLocation().distanceTo(target) < 0.01){
			return INVALID_LOCATION;
		}
		return target;
	}

	/**
	 * @param x (int) integer to convert to RobotType
	 * @return (RobotType) according to integer code
	 * code-to-type conversion:
	 * 0 -- ARCHON
	 * 1 -- GARDENER
	 * 2 -- LUMBERJACK
	 * 3 -- SCOUT
	 * 4 -- SOLDIER
	 * 5 -- TANK
	 */

	static RobotType intToType(int x){
		switch (x){
			case 0:
				return RobotType.ARCHON;
			case 1:
				return RobotType.GARDENER;
			case 2:
				return RobotType.LUMBERJACK;
			case 3:
				return RobotType.SCOUT;
			case 4:
				return RobotType.SOLDIER;
			case 5:
				return RobotType.TANK;
		}
		// Something fucked up, perchance...
		return null;
	}

	/**
	 * @param r (RobotType) type of robot to convert to integer
	 * @return (int) integer code for input robot type
	 * code-to-type conversion:
	 * 0 -- ARCHON
	 * 1 -- GARDENER
	 * 2 -- LUMBERJACK
	 * 3 -- SCOUT
	 * 4 -- SOLDIER
	 * 5 -- TANK
	 */

	static int typeToInt(RobotType r){
		switch (r){
			case ARCHON:
				return 0;
			case GARDENER:
				return 1;
			case LUMBERJACK:
				return 2;
			case SCOUT:
				return 3;
			case SOLDIER:
				return 4;
			case TANK:
				return 5;
		}
		// Something fucked up, perchance...
		return -1;
	}

	/**
	 *
	 * @param ours (RobotType) type of our robot querying distance
	 * @param theirs (RobotType) type of enemy robot to keep distance from
	 * @return (float) optimal distance to keep between our robot and their robot
	 */

	static float getOptimalDist(RobotType ours, RobotType theirs){
		switch (ours){
			case ARCHON: case GARDENER:
				return (float)7.0;
			case LUMBERJACK:
				switch (theirs){
					case ARCHON: case GARDENER: case LUMBERJACK: case SCOUT: case SOLDIER:
						return (float)2.05;
					case TANK:
						return (float)3.05;
				}
			case SCOUT:
				switch (theirs){
					case ARCHON:
						return (float)3.05;
					case GARDENER:case SCOUT:
						return (float)2.05;
					case LUMBERJACK:
						return (float)5.05;
					case SOLDIER: case TANK:
						return (float)12.05;
				}
			case SOLDIER:
				switch (theirs){
					case ARCHON: case GARDENER: case SCOUT:
						return (float)2.05;
					case LUMBERJACK: case SOLDIER:
						return (float)3.55;
					case TANK:
						return (float)6.05;
				}
			case TANK:
				switch (theirs){
					case ARCHON: case GARDENER: case LUMBERJACK: case SCOUT: case SOLDIER:
						return (float)3.05;
					case TANK:
						return (float)4.05;
				}
		}
		// Something fucked up, perchance...
		return (float)0;
	}

	/**
	 * @param ours (RobotType) type of friendly unit querying this function
	 * @param theirs (RobotType) enemy unit being queried
	 * @return (int) priority code of enemy unit for targeting purposes
	 * 				-- lower priority code is prioritized higher
	 */

	static int getPriority(RobotType ours, RobotType theirs){
		switch(ours){
			case ARCHON:case GARDENER:
				return 0;
			case LUMBERJACK:case SOLDIER:case TANK:
				switch(theirs){
					case ARCHON:
						return 5;
					case GARDENER:
						return 4;
					case LUMBERJACK: case SOLDIER: case TANK:
						return 2;
					case SCOUT:
						return 1;
				}
			case SCOUT:
				switch(theirs){
					case ARCHON:
						return 20;
					case GARDENER:
						return 1;
					case LUMBERJACK:
						return 10;
					case SCOUT:
						return 100;
					case SOLDIER:case TANK:
						return 50;
				}
		}
		// Something fucked up, perchance...
		return 0;
	}

	/**
	 * @param center (MapLocation) location of center of circle
	 * @param radius (float) radius of circle
	 * @param start (MapLocation) line segment start
	 * @param end (MapLocation) line segment end
	 * @return (boolean) determines if the line segment with endpoints "start" and "end"
	 * 					intersects the interior (not just boundary) of the circle
	 * 					centered at "center" with radius "radius"
	 * @throws GameActionException
	 */

	static boolean CircleIntersectsLine(MapLocation center, float radius, MapLocation start, MapLocation end) throws GameActionException{
		try{
			if(start.distanceTo(center) < radius){
				return true;
			}
			if(end.distanceTo(center) < radius){
				return true;
			}
			float segment_length = (float)Math.sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y));
			float dx = (end.x - start.x) / segment_length;
			float dy = (end.y - start.y) / segment_length;
			float t = dx * (center.x - start.x) + dy * (center.y - start.y); // project of center to segment
			float closest_x;
			float closest_y;
			if(t < 0){
				closest_x = start.x;
				closest_y = start.y;
			}
			else if(t > segment_length){
				closest_x = end.x;
				closest_y = end.y;
			}
			else{
				closest_x = start.x + t * dx;
				closest_y = start.y + t * dy;
			}
			float circ_to_closest = (float)Math.sqrt((center.x - closest_x) * (center.x - closest_x) + (center.y - closest_y) * (center.y - closest_y));
			if(circ_to_closest < radius){
				return true;
			}
			return false;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Finds all neutral trees that the robot can shake; shakes the tree with the most bullets.
	 * @throws GameActionException
	 */

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

	/**
	 * If we have more than 40 robots, or we are at 2800 rounds, or we have enough bullets to win,
	 * donate as many bullets as we can.
	 * @throws GameActionException
	 */

	static void checkForStockpile() throws GameActionException{
		try{
			if(rc.getTeamBullets() < 10){
				return;
			}
			if(rc.getRobotCount() > 40 || rc.getRoundNum() > 2800 || rc.getTeamBullets() > 10000 - 10 * rc.getTeamVictoryPoints()){
				rc.donate(rc.getTeamBullets() - (rc.getTeamBullets() % 10));
			}
		} catch(Exception e){
			System.out.println("checkForStockpile() error");
			e.printStackTrace();
		}
	}

	/**
	 * @return (Direction) randomly chosen Direction, produced from rotating absolute_right by an integer
	 * 					number of times by 2*PI/num_angles.
	 */


	static Direction randomDirection() {
		int rand_idx = (int)(num_angles * Math.random());
		return absolute_right.rotateLeftRads(potential_angles[rand_idx]);
	}
}



