package scoutrushwithsoldiers;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
	final static int num_slices = 10; // The number of angles a robot will consider to determine shooting location etc.
	final static int num_gardener_slices = 36;
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
		RobotPlayer.invalid_location = new MapLocation(0, 0);
		RobotPlayer.potential_angles = new float[num_slices];
		for(int i = 0; i < num_slices; i++){
			potential_angles[i] = i * 2 * (float)Math.PI / num_slices;
		}
		
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

		rc.broadcast(900, rc.readBroadcast(900) + 1); // Increment the number of archons our team has created
		
		if(rc.readBroadcast(900) == 1){
			// This is our first archon; initialize an enemy target for combat units to orient towards
			int encode = (int)their_archons[0].x * 1000 + (int)their_archons[0].y;
			rc.broadcast(500, encode);
			rc.broadcast(502, 1);
		}
		
		Direction rand = randomDirection();
		int saved_target_id = -1; // The id of the last enemy seen
		
        while (true) {
			
            try {
				// TODO: change updateSurroundings() to use less bytecodes
				// updateSurroundings();
				findShakableTrees(); // Find and shake most valuable tree
				checkForStockpile(); // Check to see if we should donate
				
				if(rc.getRobotCount() > 40 || rc.getRoundNum() > 2800 || rc.getTeamBullets() > 10000 - 10 * rc.getTeamVictoryPoints()){
					// If we have a big army, the game is sufficiently late, or we can immediately win, donate all our bullets
					rc.donate(rc.getTeamBullets() - (rc.getTeamBullets() % 10));
				}
				
				int data = rc.readBroadcast(500); // read location of enemy sighting
				MapLocation last_seen = new MapLocation(data / 1000, data % 1000); // translate location of enemy sighting to a MapLocation
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
				
				if(enemies.length == 0){
					// No enemies found
					if(rc.getLocation().distanceTo(last_seen) < getSightRadius()){
						// There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
						rc.broadcast(500, 0);
						rc.broadcast(501, 0);
						rc.broadcast(502, 0);
					}
				}
				else{
					// Enemies found, update last sighting to first enemy seen
					int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(502, RobotTypeToInt(enemies[0].getType()));
					if(enemies[0].getID() != saved_target_id){
						// If this is actually a new enemy (i.e. we didn't see them last turn) then broadcast that help is required
						saved_target_id = enemies[0].getID();
						rc.broadcast(501, 0);
					}
				}
				
				// Check all angles around us for potential build locations				
				for(int i = 0; i < num_slices; i++){
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
				
				// Try to move in current direction; if it fails, find a new direction to move in
				int trials = 0;
				while(!rc.canMove(rand) && trials < 10){
					rand = randomDirection();
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

	static void runGardener() throws GameActionException {
		
		rc.broadcast(901, rc.readBroadcast(901) + 1); // Increment the number of gardeners our team has created

		int spots_available = 0; // The number of places around our gardener we can build in
		int saved_target_id = -1; // The ID of the last enemy we've seen

        while (true) {

            try {
				//updateSurroundings();
				//findShakableTrees();
				
				int data = rc.readBroadcast(500); // read location of enemy sighting
				MapLocation last_seen = new MapLocation(data / 1000, data % 1000); // translate location of enemy sighting to a MapLocation
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // find all nearby enemies
				
				if(enemies.length == 0){
					// No enemies found
					if(rc.getLocation().distanceTo(last_seen) < getSightRadius()){
						// There was supposedly a robot sighting at last_seen, but this robot knows it's dead/gone, so update the last sighting to null
						rc.broadcast(500, 0);
						rc.broadcast(501, 0);
						rc.broadcast(502, 0);
					}
				}
				else{
					// Enemies found, update last sighting to first enemy seen
					int encode = (int) enemies[0].getLocation().x * 1000 + (int) enemies[0].getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(502, RobotTypeToInt(enemies[0].getType()));
					if(enemies[0].getID() != saved_target_id){
						// If this is actually a new enemy (i.e. we didn't see them last turn) then broadcast that help is required
						saved_target_id = enemies[0].getID();
						rc.broadcast(501, 0);
					}
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
				for(int i = 0; i < num_gardener_slices; i++){
					Direction next_build = absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices);
					//System.out.println("Checking location... " + rc.getLocation().add(next_build,2) + " from " + rc.getLocation());
					if(rc.canMove(next_build, 2)){
						spots_available++;
					}
				}
				
				//System.out.println("There are " + spots_available + " spots available from " + rc.getLocation());
				
				if(spots_available > 1 && rc.getRoundNum() > 40 && rc.getTreeCount() < 3 * rc.readBroadcast(904)){
					// Leave a location open to build combat units in, and don't build a tree if our army is weak
					// System.out.println("Building tree...");
					for(int i = 0; i < num_gardener_slices; i++){
						if(rc.canPlantTree(absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices))){
							rc.plantTree(absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices));
						}
					}
				}
				else{
					// Build either a soldier or a scout, depending on the makeup of our army and the game time
					for(int i = 0; i < num_gardener_slices; i++){
						if((rc.readBroadcast(903) < 20) && rc.canBuildRobot(RobotType.SOLDIER, absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices)) && (rc.readBroadcast(903) < rc.readBroadcast(904) + 1 || rc.getRoundNum() > 400)){
							rc.buildRobot(RobotType.SOLDIER, absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices));
						}
						if((rc.readBroadcast(904) < 20) && rc.canBuildRobot(RobotType.SCOUT, absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices)) && rc.readBroadcast(904) < 3 * rc.readBroadcast(903) + 3 && (rc.getRoundNum() < 400 || rc.getRoundNum() > 700)){
							//System.out.println("Building scout...");
							rc.buildRobot(RobotType.SCOUT, absolute_right.rotateLeftRads(i * 2 * (float)Math.PI/num_gardener_slices));
						}
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

    /*static void runSoldier() throws GameActionException {
        
		rc.broadcast(902, rc.readBroadcast(902) + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				updateSurroundings();
				findShakableTrees();
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        
		rc.broadcast(903, rc.readBroadcast(903) + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				updateSurroundings();
				findShakableTrees();
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
	
	static void runCombatUnit() throws GameActionException{
		
		// *** HANDLES BOTH SCOUT AND SOLDIER LOGIC ***
		
		if(rc.getType() == RobotType.SCOUT){
			rc.broadcast(904, rc.readBroadcast(904) + 1); // Increment the number of scouts we've created this game
		}
		else{
			rc.broadcast(903, rc.readBroadcast(903) + 1); // Increment the number of soldiers we've created this game
		}
		Direction rand = randomDirection();
		MapLocation secondary_target = invalid_location; // A previous sighting that we will head towards if we don't personally see an enemy
		RobotInfo target_robot = null;
		int target_id = -1;
		MapLocation target_loc = invalid_location;
		int saved_target_id = -1;
		int patience = 0; // Will only try to move towards a previous sighting for so long before we give up
		int PATIENCE_LIMIT = 10; // Change to affect how long we focus on a sighting
		boolean has_crossed_midline = false;
		
		while(true) {
			
			try{
				//System.out.println("Before updating surroundings: " + Clock.getBytecodeNum());
				//updateSurroundings();
				//System.out.println("After updating surroundings: " + Clock.getBytecodeNum());
				findShakableTrees();
				
				//System.out.println("My beginning-of-turn secondary target is... " + secondary_target);
				
				if(isValidLoc(secondary_target)){
					patience++;
					if(rc.getLocation().distanceTo(secondary_target) < getSightRadius() - 2){
						// We've reached previous sighting, update broadcast accordingly
						rc.broadcast(500, 0);
						rc.broadcast(501, 0);
						rc.broadcast(502, 0);
						secondary_target = invalid_location;
						saved_target_id = -1;
					}
					if(patience == PATIENCE_LIMIT){
						// Spent too long trying to move towards sighting, conclude that path is too blocked and give up
						secondary_target = invalid_location;
						patience = 0;
						saved_target_id = -1;
					}
				}
				
				int last_seen = rc.readBroadcast(500);
				int responders = rc.readBroadcast(501);
				int last_type = rc.readBroadcast(502);
				
				if(!isValidLoc(secondary_target)){
					// We aren't currently moving towards a target
					if(last_seen != 0 && responders < 5){
						// Only send 5 units to each sighting
						if(rc.getRoundNum() > 500 || (rc.getType() == RobotType.SCOUT && last_type != 4) || (rc.getType() == RobotType.SOLDIER && last_type != 2)){
							// Scouts don't respond to scouts early game, nor soldiers to gardeners
							rc.broadcast(501, rc.readBroadcast(501) + 1); // Broadcast that unit is responding to sighting
							secondary_target = new MapLocation(last_seen / 1000, last_seen % 1000);
						}
					}
				}
				
				//System.out.println("Now my secondary target is... " + secondary_target);
				
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, ENEMY); // Find nearby robots
								
				// Check to see if our current target is still alive/visible
				boolean targetAlive = false;
				for(int i = 0; i < enemies.length; i++){
					if(enemies[i].getID() == target_id){
						targetAlive = true;
						target_loc = enemies[i].getLocation();
						target_robot = enemies[i];
					}
				}
				if(!targetAlive){
					// Reset target
					target_id = -1;
					target_loc = invalid_location;
					target_robot = null;
				}
				
				//System.out.println("My target has moved to " + target_loc);
				
				if(enemies.length > 0){
					// Find the highest priority enemy in range
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
					// If the highest priority robot still isn't a priority in the early game, reset target
					if(rc.getType() == RobotType.SCOUT && (target_robot.getType() == RobotType.ARCHON || target_robot.getType() == RobotType.SCOUT)){
						// Too hard for scouts to hit scouts, takes too long for scouts to damage archons
						target_id = -1;
						target_loc = invalid_location;
						target_robot = null;
					}
					if(rc.getType() == RobotType.SOLDIER && (target_robot.getType() == RobotType.ARCHON)){
						// Takes too long to damage archons
						target_id = -1;
						target_loc = invalid_location;
						target_robot = null;
					}
				}
				
				if(target_id != -1){
					// Target exists, broadcast sighting to team
					int encode = (int)target_robot.getLocation().x * 1000 + (int)target_robot.getLocation().y;
					rc.broadcast(500, encode);
					rc.broadcast(502, RobotTypeToInt(target_robot.getType()));
					if(target_id != saved_target_id){
						rc.broadcast(501, 1); // This unit is responding already
						saved_target_id = target_id;
					}
				}
				
				MapLocation target; // The location unit should head towards in order to shoot at enemy, assuming one exists
				
				if(target_id != -1){
					target = getBestShootingLocation(target_robot);
				}
				else{
					target = invalid_location;
				}
				/*if(rc.getRoundNum() > 100 && rc.getRoundNum() < 150 && target_id != -1){
					System.out.println(rc.getLocation() + " " + target_robot.getType() + " " + target);
				}*/
				
				//System.out.println("Made it D");
				
				//System.out.println("I am at " + rc.getLocation());
				//System.out.println("I want to move to " + target);
				
				if(isValidLoc(target)){
					// Our move target is valid
					//System.out.println("Fortunately " + target + " is valid");
					//System.out.println("Target acquired... " + Clock.getBytecodeNum());
					if(rc.canMove(target)){
						// Unit can move towards its move target. Should be guaranteed but for some reason isn't (TODO?).
						//System.out.println("I indeed can move to " + target);
						rc.move(target);
						//System.out.println("I have now moved to " + rc.getLocation());
						if(rc.getLocation().distanceTo(target) < 0.5 || (rc.getLocation().distanceTo(target) < 1.5 && rc.getType() == RobotType.SOLDIER)){
							// If we got close enough to our move target, shoot at enemy with everything we've got
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
						// Unit cannot move towards its move target
						//System.out.println("But sadly, I cannot move to " + target);
						
						// Try to move towards secondary target instead
						if(isValidLoc(secondary_target) && rc.canMove(secondary_target)){
							rc.move(secondary_target);
							rand = randomDirection();
						}
						else{
							// Move in current direction, update direction with a random one if impossible
							int trials = 0;
							while((!rc.canMove(rand) || willBeHit(rand)) && trials < 10){
								rand = randomDirection();
								trials++;
							}
							if(rc.canMove(rand)){
								rc.move(rand);
							}
						}
					}
				}
				else{
					// Our move target is invalid
					
					// Get tree lists
					TreeInfo[] trees = rc.senseNearbyTrees(-1, NEUTRAL);
					TreeInfo[] trees2 = rc.senseNearbyTrees(-1, ENEMY);
					
					// If game is late, clear out trees with bullets
					if(rc.getRoundNum() > 1500 && (trees.length > 0 || trees2.length > 0)){
						TreeInfo best;
						if(trees.length > 0){
							best = trees[0];
						}
						else{
							best = trees2[0];
						}
						// Get closest tree
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
							// Head towards closest tree and shoot it down
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
							// Move in current direction, update with random direction if impossible
							int trials = 0;
							while((!rc.canMove(rand) || willBeHit(rand)) && trials < 10){
								rand = randomDirection();
								trials++;
							}
							if(rc.canMove(rand)){
								rc.move(rand);
							}
						}
					}
					else if(!isValidLoc(target_loc) || rc.getType() == RobotType.SOLDIER){
						// Game is not late, no trees found, no enemy found. Soldiers move in current direction/randomly
						if(isValidLoc(secondary_target) && rc.canMove(secondary_target)){
							rc.move(secondary_target);
							rand = randomDirection();
						}
						else{
							int trials = 0;
							while((!rc.canMove(rand) || willBeHit(rand)) && trials < 10){
								rand = randomDirection();
								trials++;
							}
							if(rc.canMove(rand)){
								rc.move(rand);
							}
						}
					}
					else{
						// Game is not late, no trees found, but an enemy is found. Scouts try to move somewhere near the enemy
						MapLocation to_move_to = invalid_location;
						int target_trials = 0;
						while(!rc.canMove(to_move_to) && target_trials < 10){
							to_move_to = target_loc.add(randomDirection(), getSightRadius());
						}
						if(isValidLoc(to_move_to)){
							rc.move(to_move_to);
						}
						else{
							// Can't move near the enemy, give up and move in current direction/randomly
							int trials = 0;
							while((!rc.canMove(rand) || willBeHit(rand)) && trials < 10){
								rand = randomDirection();
								trials++;
							}
							if(rc.canMove(rand)){
								rc.move(rand);
							}
						}
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
	
	static int RobotTypeToInt(RobotType r){
		if(r == RobotType.ARCHON){
			return 1;
		}
		if(r == RobotType.GARDENER){
			return 2;
		}
		if(r == RobotType.SOLDIER){
			return 3;
		}
		if(r == RobotType.SCOUT){
			return 4;
		}
		if(r == RobotType.LUMBERJACK){
			return 5;
		}
		return 0;
	}
	
	/**
	 * Checks to see if team should donate bullets
	 */	
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
	
	/**
	 * Check if given location is valid
	 * @param loc The location to be checked
	 * @return true if the location is valid
	 */
	static boolean isValidLoc(MapLocation loc){
		if(loc.x == invalid_location.x && loc.y == invalid_location.y){
			return false;
		}
		return true;
	}
	
	/**
	 * Finds and shakes the most valuable tree in range
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
	
	/**
	 * Updates enemy locations. TOO EXPENSIVE. TODO: FIX
	 */
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
	
	/**
	 * Gets closest target to the robot, based off of broadcasted positions. DEPENDS ON BROADCAST SYSTEM WORKING. TODO: FIX
	 */	
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
	
	/**
	 * Gets the best location to shoot at a target enemy. Attempts to dodge bullets in the process.
	 * @param target_robot The enemy being targeted
	 * @return best The best location to shoot at enemy from, or invalid_location if none exists.
	 */
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
			
			
			// Determine how far away our robot wants to be from the enemy. Scouts want to kite, soldiers dgaf
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
			
			// Initialize angles to be checked for clear lines
			boolean[] clear_angles = new boolean[num_slices];
			for(int i = 0; i < num_slices; i++){
				clear_angles[i] = true;
			}
			
			//System.out.println("Initialized clear angles... " + Clock.getBytecodeNum());
			
			// Check which angles are blocked by robots
			for(int i = 0; i < robots.length; i++){
				if(target.distanceTo(robots[i].getLocation()) > look_distance){
					// Robot is too far way, won't block our shot
					continue;
				}
				if(target.distanceTo(robots[i].getLocation()) < 0.5){
					// Robot can't block itself
					continue;
				}
				//System.out.println("Finding clear angles around robot " + i + "... " + Clock.getBytecodeNum());
				
				// Get the minimum and maximum angle that this robot blocks
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
					
					// Check if potential angle is blocked by robot
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
					// Tree is either too far away, or we're a scout targeting a gardener in which case trees are irrelevant
					continue;
				}
				// Get the minimum and maximum angle that this tree blocks
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
					// Check if potential angle is blocked by tree
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
					// Angle is blocked
					continue;
				}
				// Translate angle into a location
				Direction potential_dir = absolute_right.rotateLeftRads(potential_angles[i]);
				MapLocation potential_shooting_loc = target.add(potential_dir, look_distance);
				if(rc.canMove(potential_shooting_loc)){
					// Location is valid, check if moving there will result in getting hit by a bullet
					boolean will_add = true;
					for(int j = 0; j < bullets.length; j++){
						if(willCollideWithLoc(rc.getLocation().add(rc.getLocation().directionTo(potential_shooting_loc), (float)Math.min(2.5, rc.getLocation().distanceTo(potential_shooting_loc))), bullets[j])){
							will_add = false;
						}
					}
					// Check if moving there will result in being too close to a lumberjack
					for(int j = 0; j < robots.length; j++){
						if(robots[j].getType() == RobotType.LUMBERJACK){
							if(potential_shooting_loc.distanceTo(robots[j].getLocation()) < 3.55){
								will_add = false;
							}
						}
					}
					if(will_add){
						// Can safely move to location
						potential_loc.add(potential_shooting_loc);
					}
				}
			}
			
			//System.out.println("Calculated potential shooting spots... " + Clock.getBytecodeNum());
			
			if(potential_loc.size() == 0){
				// No good shooting locations
				//System.out.println("No shooting location found, " + Clock.getBytecodeNum());
				return invalid_location;
			}
			else{
				// Get closest shooting location
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
	
	/**
	 * Gets the sight radius of current robot
	 * @return The sight radius of current robot
	 */
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
	
	/**
	 * Determine whether moving in a given direction will result in being hit by a bullet
	 * @param dir The direction to move in
	 * @return true If moving in that direction will result in being hit by a bullet
	 */
	
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
     * Checks if a given location is in danger of being hit by a bullet
     *
     * @param bullet The bullet in question
	 * @param loc The location to be checked
     * @return True if the line of the bullet's path intersects with given location.
     */
    static boolean willCollideWithLoc(MapLocation loc, BulletInfo bullet) {
        MapLocation myLocation = loc;
		
		if(myLocation.distanceTo(bullet.location) >= 6){ 
			// Bullet is too far away, no need to bother with it now
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
