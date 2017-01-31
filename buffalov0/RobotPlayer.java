package buffalov0;
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

	// Bugpathing variables
	static Direction bugPath_previous_velocity;
	static MapLocation bugPath_closest_point;
	static MapLocation bugPath_path_destination;
	static int bugPath_algorithm_status;
	static int bugPath_patience;


	/**
	 * BROADCAST DIRECTORY
	 *
	 * [500,505] - last observed location of ENEMY unit
	 * 500 - archon
	 * 501 - gardener
	 * 502 - lumberjack
	 * 503 - scout
	 * 504 - soldier
	 * 505 - tank
	 *
	 * [900,905] - estimated # of FRIEND units
	 * 900 - archons
	 * 901 - gardeners
	 * 902 - lumberjacks
	 * 903 - scouts
	 * 904 - soldiers
	 * 905 - tanks
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
		RobotPlayer.absolute_up = new Direction((float) Math.PI/2);
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
		center = new MapLocation(centerx / (2 * our_archons.length), centery / (2 * our_archons.length));

		/**
		 * Bugpathing variable initialization
		 */
		RobotPlayer.bugPath_previous_velocity = randomDirection();
		RobotPlayer.bugPath_closest_point = rc.getLocation();
		RobotPlayer.bugPath_path_destination = rc.getLocation();
		RobotPlayer.bugPath_algorithm_status = 0;
		RobotPlayer.bugPath_patience = 0;



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
				Scout.runScout(rc);
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
	 *  getArmyToTreeRatio
	 *  getArmyToGardenerRatio
	 *  getTreeToGardenerRatio
	 *  distanceToLine
	 *  bugPathToLoc
	 *  shotWillHit
	 *  getBestLocation
	 *  getPriorityTarget
	 *  updateEnemiesAndBroadcast
	 *  getBestShootingLocation
	 *  dodgeBullets
	 *  intToType
	 *  typeToInt
	 *  getOptimalDist
	 *  getPriority
	 *  circleIntersectsLine
	 *  findShakableTrees
	 *  checkForStockpile
	 *  randomDirection
	 **/

	/**
	 * getArmyToTreeRatio
	 *
	 * Returns desired army/tree ratio
	 * 	-- Currently, ratio increases linearly with time, from 1 to 4.
	 */

	public static float getArmyToTreeRatio(){
		try {
			int mapSize = rc.readBroadcast(38) - rc.readBroadcast(37);
			mapSize = mapSize / 1000 + mapSize % 1000;
			if(mapSize > 170){
				return 0.5f + 0.0001f * rc.getRoundNum();
			} else if(mapSize > 140){
				return 0.5f + 0.0002f * rc.getRoundNum();
			} else if(mapSize > 100){
				return 0.5f + 0.0003f * rc.getRoundNum();
			}
		} catch(Exception e){
			System.out.println("getArmyToTreeRatio() Exception");
			e.printStackTrace();
		}
		return 0.5f + 0.0004f * rc.getRoundNum();
	}

	/**
	 * getArmyToGardenerRatio
	 *
	 * Returns desired army/gardener ratio
	 * 	-- Calculated by other two functions
	 */

	public static float getArmyToGardenerRatio(){
		return getArmyToTreeRatio() * getTreeToGardenerRatio();
	}

	/**
	 * getTreeToGardenerRatio
	 *
	 * Returns desired tree/gardener ratio
	 * 	-- Currently, ratio is constant 3.
	 */


	public static float getTreeToGardenerRatio(){
		return 2.1f;
	}



	/**
	 * distanceToLine
	 *
	 * @param query_point (MapLocation) direction in which robot previously moved
	 * @param line_start (MapLocation) start of line segment
	 * @param line_end (MapLocation) end of line segment
	 * @return (float) distance from query_point to line
	 */

	public static float distanceToLine(MapLocation query_point,
					     MapLocation line_start,
					     MapLocation line_end){
		// Find area of triangle, solve for height from query to line
		// Area found from Shoelace thm
		float Area = Math.abs(0.5f*(line_start.x*line_end.y
						+line_end.x*query_point.y
					    +query_point.x*line_start.y
					    -line_start.y*line_end.x
					    -line_end.y*query_point.x
					    -query_point.y*line_start.x));
		float side_length = line_start.distanceTo(line_end);
		float height = 2.0f * Area / side_length;
		return height;
	}


	/**
	 * bugPathToLoc
	 *
	 * Function executes bug-pathing.
	 * Algorithm -- Keep running tally of closest point it's been to the target.
	 * If it's the closest it's ever been, keep going toward the target.
	 * If it can't, then circumvent the obstacle until we're closer than we've ever been again, and keep going to the target.
	 *
	 * Resets the algorithm every 100 turns.
	 *
	 * @param target (MapLocation) where to bugpath towards
	 *  -- Uses the four bugpathing variables declared above
	 */

	public static void bugPathToLoc(MapLocation target){
		try {
			if (!RobotPlayer.bugPath_path_destination.equals(target)){
				RobotPlayer.bugPath_path_destination = target;
				RobotPlayer.bugPath_closest_point = rc.getLocation();
				RobotPlayer.bugPath_algorithm_status = 0;
			}
			if (rc.getRoundNum() % 100 == 0){
				RobotPlayer.bugPath_closest_point = rc.getLocation();
				RobotPlayer.bugPath_algorithm_status = 0;
			}
			Direction previous_velocity = RobotPlayer.bugPath_previous_velocity;
			MapLocation closest_point = RobotPlayer.bugPath_closest_point;
			MapLocation previous_location = rc.getLocation();
			if (target.equals(rc.getLocation())){
				// We're done bugpathing and don't need to move
			}
			else {
				/**
				 * STATUS DESIGNATIONS
				 * 0 - Closest it's ever been, not blocked
				 * 1 - Blocked, working its way around
				 */

				// Update Status
				if (previous_location.distanceTo(target) <= closest_point.distanceTo(target)){
					RobotPlayer.bugPath_closest_point = previous_location;
					if (rc.canMove(target)) {
						// We're as close as ever and nothing is stopping us now!
						RobotPlayer.bugPath_algorithm_status = 0;
					}
					else{
						// We just found an obstacle - turn to the left and try to circumvent it
						RobotPlayer.bugPath_algorithm_status = 1;
						for (int i = 0; i < num_angles; i++){
							Direction potential_direction = previous_velocity.rotateLeftDegrees(360.0f * i / num_angles);
							if (rc.canMove(potential_direction) && !rc.hasMoved()) {
								rc.move(potential_direction);
								RobotPlayer.bugPath_previous_velocity = previous_location.directionTo(rc.getLocation());
							}
						}
					}
				}
				else {
					RobotPlayer.bugPath_algorithm_status = 1;
				}

				System.out.println("STATUS == " + String.valueOf(RobotPlayer.bugPath_algorithm_status));

				// Move
				if (RobotPlayer.bugPath_algorithm_status == 0){
					// We're as close as ever and nothing is stopping us now!
					if (!rc.hasMoved()) {
						rc.move(target);
						RobotPlayer.bugPath_previous_velocity = previous_location.directionTo(rc.getLocation());
					}
				}
				else{
					// We're currently circumventing an obstacle.
					// Scan angles from where we just moved from working counterclockwise, until we sense an obstacle
					boolean obstacle_found = false;
					Direction potential_direction = null;
					// Dummy variable
					int d = 0;
					for (int i = 0; i < num_angles; i++) {
						d = i;
						potential_direction = previous_velocity.rotateRightDegrees(180.0f - 360.0f * i / num_angles);
						if (!rc.canMove(potential_direction)) {
							// We've found the obstacle
							obstacle_found = true;
							break;
						}
					}
					if (!obstacle_found && !rc.hasMoved()){
						rc.move(target);
						RobotPlayer.bugPath_previous_velocity = previous_location.directionTo(rc.getLocation());
					}
					// Keep scanning until we stop sensing the obstacle
					boolean egress_found = false;
					for (int i = d; i < num_angles; i++) {
						potential_direction = previous_velocity.rotateRightDegrees(180.0f- 360.0f * i / num_angles);
						if (rc.canMove(potential_direction)) {
							// We've found a method of egress
							egress_found = true;
							break;
						}
					}
					if (egress_found && !rc.hasMoved()) {
						rc.move(potential_direction);
						RobotPlayer.bugPath_previous_velocity = previous_location.directionTo(rc.getLocation());
					}
					if (rc.getLocation().distanceTo(target) < closest_point.distanceTo(target)){
						RobotPlayer.bugPath_closest_point = rc.getLocation();
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * getBestLocation
	 *
	 * @return Best location according to heuristic
	 * 			-- If you're a scout and it's early, rush their archons
	 * 			-- Otherwise, if you're a combat unit, rush highest priority enemy
	 * 			-- Otherwise, dodge bullets if it's not early
	 * 			-- Otherwise, move towards the last seen enemy of highest priority
	 * @throws GameActionException
	 */

	public static MapLocation getBestLocation() throws GameActionException {
		MapLocation best_location;

		// If you're a scout and it's early, rush their archons, because their gardener is probably nearby
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
			RobotInfo priority_target = getPriorityTarget();
			best_location = getBestShootingLocation(priority_target);
			if (!best_location.equals(INVALID_LOCATION) && rc.canMove(best_location)) {
				//System.out.println("Found a good shooting location, going to " + best_location + " used " + Clock.getBytecodeNum());
				return best_location;
			}
		}

		//System.out.println("Found no good shooting location. Dodging bullets... " + Clock.getBytecodeNum());
		/*if (rc.getRoundNum() > 100){
			best_location = dodgeBullets();
			if(best_location.x != INVALID_LOCATION.x && rc.canMove(best_location)){
				//System.out.println("Dodged " + bullets.length + " bullets using " + Clock.getBytecodeNum() + " going towards " + best_location);
				return best_location;
			}
		}*/

		//use secondary targets and find which type is the one we want to target
		int priorityType = 0;
		for(int i = 1; i < last_sighting_location.length; i++) {
			if(!last_sighting_location[i].equals(INVALID_LOCATION)
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
	 * getPriorityTarget
	 *
	 * @return (RobotInfo) the robot in RobotPlayer.enemies with the highest priority relative to rc.
	 * 						-- If multiple robots have highest priority, chooses the first one.
	 * @throws GameActionException
	 */

	public static RobotInfo getPriorityTarget() throws GameActionException{
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
	 * updateEnemiesAndBroadcast
	 *
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

		//Update map size if we've reached past WHERE WE KNOW
		int lowerLeft = rc.readBroadcast(37);
		int upperRight = rc.readBroadcast(38);
		MapLocation ours = rc.getLocation();
		if(ours.x < lowerLeft / 1000){
			rc.broadcast(37, ((int)(ours.x)) * 1000 + (lowerLeft % 1000));
		}
		if(ours.y < lowerLeft % 1000){
			rc.broadcast(37, (lowerLeft / 1000) * 1000 + (int)ours.y);
		}
		if(ours.x > upperRight / 1000){
			rc.broadcast(38, ((int)(ours.x)) * 1000 + (upperRight % 1000));
		}
		if(ours.y > upperRight % 1000){
			rc.broadcast(38, (upperRight / 1000) * 1000 + (int)ours.y);
		}
	}

	/**
	 * getBestShootingLocation
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
			if(rc.canMove(potential_shooting_loc) && CombatStrategy.shotWillHit(potential_shooting_loc, priority_target)){
				return potential_shooting_loc;
			}
		}

		return INVALID_LOCATION;
	}

	/**
	 * dodgeBullets
	 *
	 * @return (Location) a location that is out of the way of each bullet the robot has senced
	 * @throws GameActionException
	 */

	static MapLocation dodgeBullets() throws GameActionException{
		//int orig_bytecodes = Clock.getBytecodeNum();
		float vector_x = 0f;
		float vector_y = 0f;

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
	 * intToType
	 *
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
	 * typeToInt
	 *
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
	 * getOptimalDist
	 *
	 * @param ours (RobotType) type of our robot querying distance
	 * @param theirs (RobotType) type of enemy robot to keep distance from
	 * @return (float) optimal distance to keep between our robot and their robot
	 */

	static float getOptimalDist(RobotType ours, RobotType theirs){
		switch (ours){
			case ARCHON: case GARDENER:
				return 7.0f;
			case LUMBERJACK:
				switch (theirs){
					case ARCHON: case GARDENER: case LUMBERJACK: case SCOUT: case SOLDIER:
						return 2.05f;
					case TANK:
						return 3.05f;
				}
			case SCOUT:
				switch (theirs){
					case ARCHON:
						return 3.05f;
					case GARDENER:case SCOUT:
						return 2.05f;
					case LUMBERJACK:
						return 5.05f;
					case SOLDIER: case TANK:
						return 12.05f;
				}
			case SOLDIER:
				switch (theirs){
					case ARCHON: case GARDENER: case SCOUT:
						return 2.05f;
					case LUMBERJACK:
						return 5.55f;
					case SOLDIER:
						return 6.05f;
					case TANK:
						return 8.75f;
				}
			case TANK:
				switch (theirs){
					case ARCHON: case GARDENER: case LUMBERJACK: case SCOUT: case SOLDIER:
						return 3.05f;
					case TANK:
						return 4.05f;
				}
		}
		// Something fucked up, perchance...
		return 0f;
	}

	/**
	 * getPriority
	 *
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
						return 10;
					case SCOUT:
						return 5;
					case GARDENER:
						return 4;
					case LUMBERJACK: case TANK:
						return 3;
					case SOLDIER:
						return 2;
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
	 * circleIntersectsLine
	 *
	 * @param center (MapLocation) location of center of circle
	 * @param radius (float) radius of circle
	 * @param start_loc (MapLocation) center of where path starts (this will always be center of rc)
	 * @param end_loc (MapLocation) center of where path ends (in general, unit will be here)
	 * @return (boolean) determines if the line segment with endpoints "start" and "end"
	 * 					intersects the interior (not just boundary) of the circle
	 * 					centered at "center" with radius "radius"
	 * @throws GameActionException
	 */

	static boolean circleIntersectsPath(MapLocation center, float radius, MapLocation start_loc, MapLocation end_loc) throws GameActionException{
		try{
			// If angle theta between center and start_loc is >= 90, the circle will never intersect.
			Direction path_direction = start_loc.directionTo(end_loc);
			float theta = Math.abs(start_loc.directionTo(center).radiansBetween(path_direction));
			if (theta >= Math.PI / 2) {
				return false;
			}
			// If center.equals(end_loc) for some reason, return false.
			// We're using this for targeting anyways...
			if (center.equals(end_loc)){
				return false;
			}
			// Else, let d be distance from start_loc to center.
			// circle intersects path if and only if d < path length
			// and d*sin(theta) < radius.
			float d = start_loc.distanceTo(center);
			return (d * Math.sin(theta) < radius && d < start_loc.distanceTo(end_loc));
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * findShakableTrees
	 *
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
	 * checkForStockpile
	 *
	 * If we have more than 40 robots, or we are at 2800 rounds, or we have enough bullets to win,
	 * donate as many bullets as we can.
	 * @throws GameActionException
	 */

	static void checkForStockpile() throws GameActionException{
		try{
			if(rc.getTeamBullets() < rc.getVictoryPointCost()){
				return;
			}
			if(rc.getRoundNum() > 2800){
				rc.donate(rc.getTeamBullets() - rc.getTeamBullets() % rc.getVictoryPointCost());
			}
			if(rc.getTeamBullets() > (1000 - rc.getTeamVictoryPoints()) * rc.getVictoryPointCost()){
				rc.donate(rc.getTeamBullets() - rc.getTeamBullets() % rc.getVictoryPointCost());
			}
			if(rc.getTeamBullets() > 500){
				rc.donate(rc.getTeamBullets() - 500 - (rc.getTeamBullets() - 500) % rc.getVictoryPointCost());
			}
		} catch(Exception e){
			System.out.println("checkForStockpile() error");
			e.printStackTrace();
		}
	}

	/**
	 * randomDirection
	 *
	 * @return (Direction) randomly chosen Direction, produced from rotating absolute_right by an integer
	 * 					number of times by 2*PI/num_angles.
	 */


	static Direction randomDirection() {
		int rand_idx = (int)(num_angles * Math.random());
		return absolute_right.rotateLeftRads(potential_angles[rand_idx]);
	}
}



