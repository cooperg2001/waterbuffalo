package smartmoving;
import battlecode.common.*;
import java.util.*;
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
	final static int num_angles = 10;
	final static int num_steps = 5;
	static RobotInfo[] robots;
	static RobotInfo[] enemies;
	static TreeInfo[] trees;
	static TreeInfo[] neutral_trees;
	static TreeInfo[] their_trees;
	static TreeInfo[] our_trees;
	static BulletInfo[] bullets;
	static MapLocation last_sighting_location;
	static RobotType last_sighting_type;
	
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
		
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runCombatUnit();
                break;
            /*case LUMBERJACK:
                runLumberjack();
                break;*/
			case SCOUT:
				runCombatUnit();
				break;
        }
	}
	
	static void runArchon() throws GameActionException {
		rc.broadcast(900, rc.readBroadcast(900) + 1);
		if(rc.readBroadcast(900) == 1){
			// This is our first archon; initialize an enemy target for combat units to orient towards
			int encode = (int)their_archons[0].x * 1000 + (int)their_archons[0].y;
			rc.broadcast(500, encode);
			rc.broadcast(501, typeToInt(RobotType.ARCHON));
		}
		
		Direction rand = randomDirection();
		
		while(true){
			try{
				findShakableTrees();
				checkForStockpile();
				
				robots = rc.senseNearbyRobots();
				enemies = rc.senseNearbyRobots(-1, ENEMY);
				trees = rc.senseNearbyTrees(-1);
				neutral_trees = rc.senseNearbyTrees(-1, NEUTRAL);
				their_trees = rc.senseNearbyTrees(-1, ENEMY);
				our_trees = rc.senseNearbyTrees(-1, FRIEND);
				bullets = rc.senseNearbyBullets();
				int last_sighting_location_encoded = rc.readBroadcast(500);
				last_sighting_location = new MapLocation(last_sighting_location_encoded / 1000, last_sighting_location_encoded % 1000);
				int last_sighting_type_encoded = rc.readBroadcast(501);
				last_sighting_type = intToType(last_sighting_type_encoded);
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
				
				if(enemies.length == 0){
					// No enemies found
					if(rc.getLocation().distanceTo(last_sighting_location) < rc.getType().sensorRadius){
						// There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
						rc.broadcast(500, 999999);
						rc.broadcast(501, 0);
					}
				}
				else{
					// Enemies found, update last sighting to first enemy seen
					int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(501, typeToInt(enemies[0].getType()));
				}
				
				// Check all angles around us for potential build locations				
				for(int i = 0; i < num_angles; i++){
					Direction dir = absolute_right.rotateLeftRads(potential_angles[i]);
					if(rc.canHireGardener(dir) && (rc.readBroadcast(904) >= 2 * rc.readBroadcast(901) - 2 || rc.getTeamBullets() > 200)){
						// We can hire a gardener, and we have a sufficiently big army to justify hiring gardeners
						// Try to make sure hiring a gardener doesn't trap our archon
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
				
				MapLocation target = get_best_location();
				if(target.x != INVALID_LOCATION.x && rc.canMove(target) && !rc.hasMoved()){
					rc.move(target);
					rand = randomDirection();
				}
				else{
					if(rc.canMove(rand) && !rc.hasMoved()){
						rc.move(rand);
					}
					else{
						int trials = 0;
						while(!rc.canMove(rand) && trials < 10){
							rand = randomDirection();
							trials++;
						}
						if(rc.canMove(rand) && !rc.hasMoved()){
							rc.move(rand);
						}
					}
				}
				
				Clock.yield();
			} catch(Exception e){
				System.out.println("Archon Exception");
                e.printStackTrace();
			}
		}
	}
	
	static void runGardener() throws GameActionException{
		rc.broadcast(901, rc.readBroadcast(901) + 1); // Increment the number of gardeners our team has created
		int spots_available = 0; // The number of places around our gardener we can build in
		
		while(true){
			try{
				findShakableTrees();
				checkForStockpile();
				
				robots = rc.senseNearbyRobots();
				enemies = rc.senseNearbyRobots(-1, ENEMY);
				trees = rc.senseNearbyTrees(-1);
				neutral_trees = rc.senseNearbyTrees(-1, NEUTRAL);
				their_trees = rc.senseNearbyTrees(-1, ENEMY);
				our_trees = rc.senseNearbyTrees(-1, FRIEND);
				bullets = rc.senseNearbyBullets();
				int last_sighting_location_encoded = rc.readBroadcast(500);
				last_sighting_location = new MapLocation(last_sighting_location_encoded / 1000, last_sighting_location_encoded % 1000);
				int last_sighting_type_encoded = rc.readBroadcast(501);
				last_sighting_type = intToType(last_sighting_type_encoded);
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
				
				if(enemies.length == 0){
					// No enemies found
					if(rc.getLocation().distanceTo(last_sighting_location) < rc.getType().sensorRadius){
						// There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
						rc.broadcast(500, 999999);
						rc.broadcast(501, 0);
					}
				}
				else{
					// Enemies found, update last sighting to first enemy seen
					int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(501, typeToInt(enemies[0].getType()));
				}
				
				TreeInfo[] tree_list = rc.senseNearbyTrees(-1, rc.getTeam()); // Find all allied trees within range
				if(tree_list.length > 0){
					// Determine tree in range with least health
					TreeInfo best = tree_list[0];
					for(int i = 0; i < tree_list.length; i++){
						if(tree_list[i].getHealth() < best.getHealth() && rc.canWater(tree_list[i].getLocation())){
							best = tree_list[i];
						}
					}
					// Water the tree in range with least health, if one exists
					if(rc.canWater(best.getLocation())){
						rc.water(best.getLocation());
					}
				}
				
				// Determine the number of available build spots				
				spots_available = 0;
				int num_gardener_slices = 6;
				for(int i = 0; i < num_gardener_slices; i++){
					Direction next_build = absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices);
					//System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
					if(rc.canMove(next_build, 2)){
						spots_available++;
					}
				}
				
				if(spots_available > 1 && rc.getRoundNum() > 40 && rc.getTreeCount() < 3 * rc.readBroadcast(904)){
					// Leave a location open to build combat units in, and don't build a tree if our army is weak
					// System.out.println("Building tree...");
					for(int i = 0; i < num_gardener_slices; i++){
						Direction next_build;
						if(i % 2 == 0){
							next_build = backward.rotateLeftRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
						}
						else{
							next_build = backward.rotateRightRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
						}
						if(rc.canPlantTree(next_build)){
							rc.plantTree(next_build);
						}
					}
				}
				else{
					// Build either a soldier or a scout, depending on the makeup of our army and the game time
					for(int i = 0; i < num_gardener_slices; i++){
						Direction next_build;
						if(i % 2 == 0){
							next_build = forward.rotateLeftRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
						}
						else{
							next_build = forward.rotateRightRads((int)(i + 1)/2 * 2 * (float)Math.PI/num_gardener_slices);
						}
						if((rc.readBroadcast(903) < 20) && rc.canBuildRobot(RobotType.SOLDIER, next_build) && (rc.readBroadcast(903) < rc.readBroadcast(904) || rc.getRoundNum() > 400)){
							rc.buildRobot(RobotType.SOLDIER, next_build);
						}
						if((rc.readBroadcast(904) < 20) && rc.canBuildRobot(RobotType.SCOUT, next_build) && rc.readBroadcast(904) < 2 * rc.readBroadcast(903) + 1 && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
							//System.out.println("Building scout...");
							rc.buildRobot(RobotType.SCOUT, next_build);
						}
					}
				}
				
				Clock.yield();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	static void runCombatUnit() throws GameActionException{
		if(rc.getType() == RobotType.SCOUT){
			rc.broadcast(904, rc.readBroadcast(904) + 1); // Increment the number of scouts we've created this game
		}
		else{
			rc.broadcast(903, rc.readBroadcast(903) + 1); // Increment the number of soldiers we've created this game
		}
		
		Direction rand = randomDirection();
		
		while(true){
			try{
				findShakableTrees();
				checkForStockpile();
				
				robots = rc.senseNearbyRobots();
				enemies = rc.senseNearbyRobots(-1, ENEMY);
				trees = rc.senseNearbyTrees(-1);
				neutral_trees = rc.senseNearbyTrees(-1, NEUTRAL);
				their_trees = rc.senseNearbyTrees(-1, ENEMY);
				our_trees = rc.senseNearbyTrees(-1, FRIEND);
				bullets = rc.senseNearbyBullets();
				int last_sighting_location_encoded = rc.readBroadcast(500);
				last_sighting_location = new MapLocation(last_sighting_location_encoded / 1000, last_sighting_location_encoded % 1000);
				int last_sighting_type_encoded = rc.readBroadcast(501);
				last_sighting_type = intToType(last_sighting_type_encoded);
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
				
				if(enemies.length == 0){
					// No enemies found
					if(rc.getLocation().distanceTo(last_sighting_location) < rc.getType().sensorRadius){
						// There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
						rc.broadcast(500, 999999);
						rc.broadcast(501, 0);
					}
				}
				else{
					// Enemies found, update last sighting to first enemy seen
					int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(501, typeToInt(enemies[0].getType()));
				}
				
				MapLocation target = get_best_location();
				if(target.x != INVALID_LOCATION.x && rc.canMove(target) && !rc.hasMoved()){
					rc.move(target);
					rand = randomDirection();
				}
				else{
					if(rc.canMove(rand) && !rc.hasMoved()){
						rc.move(rand);
					}
					else{
						int trials = 0;
						while(!rc.canMove(rand) && trials < 10){
							rand = randomDirection();
							trials++;
						}
						if(rc.canMove(rand) && !rc.hasMoved()){
							rc.move(rand);
						}
					}
				}
				
				if(enemies.length > 0){
					RobotInfo priority_target = get_priority_target();
					if(shotWillHit(rc.getLocation(), priority_target)){
						boolean should_shoot = true;
						if(priority_target.getType() == RobotType.ARCHON){
							if(rc.getRoundNum() < 500){
								should_shoot = false;
							}
						}
						if(rc.getType() == RobotType.SCOUT && (priority_target.getType() == RobotType.SCOUT)){
							if(robots.length - enemies.length < 3){
								should_shoot = false;
							}
						}
						if(rc.getType() == RobotType.SOLDIER){
							if(priority_target.getType() == RobotType.SCOUT && rc.getLocation().distanceTo(priority_target.getLocation()) > 5){
								should_shoot = false;
							}
						}
						if(rc.canFirePentadShot() && should_shoot){
							rc.firePentadShot(rc.getLocation().directionTo(priority_target.getLocation()));
						}
						if(rc.canFireTriadShot() && should_shoot){
							rc.fireTriadShot(rc.getLocation().directionTo(priority_target.getLocation()));
						}
						if(rc.canFireSingleShot() && should_shoot){
							rc.fireSingleShot(rc.getLocation().directionTo(priority_target.getLocation()));
						}
					}
				}
				
				
				Clock.yield();
			} catch(Exception e){
				e.printStackTrace();
			}
		}		
	}
	
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
	
	public static MapLocation get_best_location() throws GameActionException{
		MapLocation best_location;
		System.out.println("Finding best location... " + Clock.getBytecodeNum());
		if(enemies.length > 0){
			RobotInfo priority_target = get_priority_target();
			boolean should_shoot = true;
			if(priority_target.getType() == RobotType.ARCHON){
				if(rc.getRoundNum() < 500){
					should_shoot = false;
				}
			}
			if(rc.getType() == RobotType.SCOUT && (priority_target.getType() == RobotType.SCOUT || priority_target.getType() == RobotType.SOLDIER)){
				if(robots.length - enemies.length < 3){
					should_shoot = false;
				}
			}
			if(rc.getType() == RobotType.SOLDIER){
				if(priority_target.getType() == RobotType.SCOUT && rc.getLocation().distanceTo(priority_target.getLocation()) < 5){
					should_shoot = false;
				}
			}
			if(should_shoot){
				best_location = getBestShootingLocation(priority_target);
				System.out.println("Found a good shooting location, going to " + best_location + " used " + Clock.getBytecodeNum());
				if(best_location.x != INVALID_LOCATION.x && rc.canMove(best_location)){
					return best_location;
				}
			}
		}
		
		System.out.println("Found no good shooting location. Dodging bullets... " + Clock.getBytecodeNum());
		best_location = dodgeBullets();
		if(best_location.x != INVALID_LOCATION.x && rc.canMove(best_location)){
			System.out.println("Dodged " + bullets.length + " bullets using " + Clock.getBytecodeNum() + " going towards " + best_location);
			return best_location;
		}
		
		best_location = last_sighting_location;
		System.out.println("Found no way to dodge " + bullets.length + " bullets. Heading towards secondary target... " + best_location + " used " + Clock.getBytecodeNum());
		if(best_location.x != INVALID_LOCATION.x && rc.canMove(best_location) && rc.getType() != RobotType.ARCHON){
			if(rc.getType() == RobotType.SCOUT){
				if(rc.readBroadcast(501) != 5 || rc.getRoundNum() > 500){
					return best_location;
				}
			}
			else{
				return best_location;
			}
		}
		
		System.out.println("No secondary target. Found no good movement location, move randomly. " + Clock.getBytecodeNum());
		return INVALID_LOCATION;
	}
	
	public static RobotInfo get_priority_target() throws GameActionException{
		RobotInfo priority_target = enemies[0];
		for(int i = 0; i < enemies.length; i++){
			if(getPriority(rc.getType(), enemies[i].getType()) < getPriority(rc.getType(), priority_target.getType())){
				priority_target = enemies[i];
			}
			else if(getPriority(rc.getType(), enemies[i].getType()) == getPriority(rc.getType(), priority_target.getType()) && enemies[i].getHealth() < priority_target.getHealth()){
				priority_target = enemies[i];
			}
		}
		return priority_target;
	}
	
	//
	// TODO: Make below less expensive and/or revamp
	//
	
	/*public static MapLocation get_best_location() throws GameActionException{
		float max_move_length = rc.getType().strideRadius;
		int encoded_sighting = rc.readBroadcast(500);
		int encoded_sighting_type = rc.readBroadcast(501);
		
		// score is a heuristic for how "desirable" a location is. Larger values indicate a more desirable location.
		int[] score = new int[num_angles * num_steps]; 
		int current_score = getScore(rc.getLocation(), -1000000);
		
		MapLocation best = rc.getLocation();
		int best_score = current_score;
		for(int i = 0; i < num_angles; i++){
			for(int j = 0; j < num_steps; j++){
				Direction dir = absolute_right.rotateLeftRads(i * 2 * (float) Math.PI/num_angles);
				MapLocation new_location = rc.getLocation().add(dir, (j + 1) * max_move_length / num_steps);
				if(Clock.getBytecodeNum() < 8000){
					score[i * num_steps + j] = 10 * j + getScore(new_location, best_score - 10 * j);
					
				}
				else{
					score[i * num_steps + j] = -1000000;
				}
				if(score[i * num_steps + j] > best_score){
					best = new_location;
					best_score = score[i * num_steps + j];
				}
			}
		}
		
		System.out.println("Found best location! " + Clock.getBytecodeNum());
		
		return best;
	}
	
	public static int getScore(MapLocation loc, int score_limit) throws GameActionException{
		System.out.println("Getting score of " + loc + " " + Clock.getBytecodeNum());
		int score = 0;
		
		// Check that location is valid
		if(!rc.canMove(loc)){
			return -1000000;
		}
		
		// Close to center is slightly better
		score -= 1 * (float)rc.getLocation().distanceTo(center);
		if(score < score_limit){
			return score;
		}
		
		int bytecode = Clock.getBytecodeNum();
		// Dodge bullets	
		for(int i = 0; i < bullets.length; i++){
			MapLocation new_bullet_location = bullets[i].getLocation().add(bullets[i].getDir(), bullets[i].getSpeed());
			if(CircleIntersectsLine(loc, rc.getType().bodyRadius + (float)0.01, bullets[i].getLocation(), new_bullet_location)){
				score -= 10000;
				if(score < score_limit){
					return score;
				}
			}
		}
		System.out.println("Takes " + (Clock.getBytecodeNum() - bytecode) + " bytecodes to deal with " + bullets.length + " bullets.");
		
		// Move towards enemies
		for(int i = 0; i < enemies.length; i++){
			float dist = loc.distanceTo(enemies[i].getLocation());
			score -= 50 * Math.abs((dist - (float)getOptimalDist(rc.getType(), enemies[i].getType())) + 5) * (float)getPriority(rc.getType(), enemies[i].getType());
			if(score < score_limit){
				return score;
			}
		}
		
		// Move towards sighting
		if(last_sighting_location.x != INVALID_LOCATION.x){
			score -= 10 * loc.distanceTo(last_sighting_location) * getPriority(rc.getType(), last_sighting_type);
			if(score < score_limit){
				return score;
			}
		}
		
		return score;
	}*/
	
	static MapLocation getBestShootingLocation(RobotInfo priority_target) throws GameActionException{
		float optimalDist = getOptimalDist(rc.getType(), priority_target.getType());
		
		for(int i = 0; i < num_angles; i++){
			if(Clock.getBytecodeNum() > 8000){
				return INVALID_LOCATION;
			}
			Direction target_to_robot = priority_target.getLocation().directionTo(rc.getLocation());
			Direction potential_dir;
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
			
			// Check if moving there will result in being too close to a lumberjack
			for(int j = 0; j < robots.length; j++){
				if(robots[j].getType() == RobotType.LUMBERJACK || robots[j].getType() == RobotType.SOLDIER){
					if(step_towards_shooting_loc.distanceTo(robots[j].getLocation()) < 5.05){
						will_add_step = false;
					}
				}
				if(robots[j].getType() == RobotType.LUMBERJACK || robots[j].getType() == RobotType.SOLDIER){
					if(small_step_towards_shooting_loc.distanceTo(robots[j].getLocation()) < 5.05){
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

	static RobotType intToType(int x){
		if(x == 1){
			return RobotType.ARCHON;
		}
		if(x == 2){
			return RobotType.GARDENER;
		}
		if(x == 3){
			return RobotType.SOLDIER;
		}
		if(x == 4){
			return RobotType.TANK;
		}
		if(x == 5){
			return RobotType.SCOUT;
		}
		return RobotType.LUMBERJACK;
	}

	static int typeToInt(RobotType r){
		if(r == RobotType.ARCHON){
			return 1;
		}
		if(r == RobotType.GARDENER){
			return 2;
		}
		if(r == RobotType.SOLDIER){
			return 3;
		}
		if(r == RobotType.TANK){
			return 4;
		}
		if(r == RobotType.SCOUT){
			return 5;
		}
		if(r == RobotType.LUMBERJACK){
			return 6;
		}
		return 0;
	}

	static float getOptimalDist(RobotType ours, RobotType theirs){
		if(ours == RobotType.SCOUT){
			if(theirs == RobotType.GARDENER){
				return (float)2.05;
			}
			if(theirs == RobotType.ARCHON){
				return (float)3.05;
			}
			if(theirs == RobotType.LUMBERJACK){
				return (float)4.05;
			}
			if(theirs == RobotType.SCOUT){
				return (float)6.05;
			}
			if(theirs == RobotType.SOLDIER || theirs == RobotType.TANK || theirs == RobotType.SCOUT){
				return (float)12.05;
			}
		}
		if(ours == RobotType.LUMBERJACK){
			if(theirs == RobotType.TANK){
				return (float)3.05;
			}
			else{
				return (float)2.05;
			}
		}
		if(ours == RobotType.SOLDIER){
			if(theirs == RobotType.SCOUT || theirs == RobotType.ARCHON || theirs == RobotType.GARDENER){
				return (float)2.05;
			}
			if(theirs == RobotType.SOLDIER || theirs == RobotType.LUMBERJACK){
				return (float)3.55;
			}
			if(theirs == RobotType.TANK){
				return (float)6.05;
			}
		}
		if(ours == RobotType.ARCHON || ours == RobotType.GARDENER){
			return (float)7.0;
		}
		if(ours == RobotType.TANK){
			if(theirs == RobotType.TANK){
				return (float)4.05;
			}
			else{
				return (float)3.05;
			}
		}
		return (float)0;
	}

	static int getPriority(RobotType ours, RobotType theirs){
		if(ours == RobotType.SCOUT){
			if(theirs == RobotType.GARDENER){
				return 1;
			}
			if(theirs == RobotType.LUMBERJACK){
				return 10;
			}
			if(theirs == RobotType.ARCHON){
				return 20;
			}
			if(theirs == RobotType.SOLDIER || theirs == RobotType.TANK){
				return 50;
			}
			if(theirs == RobotType.SCOUT){
				return 100;
			}
		}
		if(ours == RobotType.SOLDIER || ours == RobotType.TANK || ours == RobotType.LUMBERJACK){
			if(theirs == RobotType.SCOUT){
				return 1;
			}
			if(theirs == RobotType.SOLDIER || theirs == RobotType.TANK || theirs == RobotType.LUMBERJACK){
				return 2;
			}
			if(theirs == RobotType.ARCHON){
				return 5;
			}
			if(theirs == RobotType.GARDENER){
				return 10;
			}
		}
		if(ours == RobotType.ARCHON || ours == RobotType.GARDENER){
			return 0;
		}
		return 0;
	}

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
					System.out.println("I am at " + rc.getLocation());
					System.out.println("Shaking tree at " + best.getLocation() + " to get " + best.getContainedBullets() + " bullets");
					rc.shake(best.getLocation());
				}
			}
		} catch(Exception e){
			System.out.println("findShakableTrees() error");
			e.printStackTrace();
		}
	}
	
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
	
	static Direction randomDirection() {
		int rand_idx = (int)(num_angles * Math.random());
        return absolute_right.rotateLeftRads(potential_angles[rand_idx]);
    }
}
