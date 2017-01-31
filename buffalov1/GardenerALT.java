package buffalov1;

import battlecode.common.*;

public class GardenerALT {
	
	static RobotController rc;
    static final float gardener_angle_gradient = 2 * (float)Math.PI / RobotPlayer.num_angles;
	static final float tree_angle_gradient = 2 * (float)Math.PI / 6;
	static MapLocation target = null;
	static boolean reached_target = false;
	static boolean need_lumberjack = false;
	static boolean built_lumberjack = false;
	static int patience = 0;
	static boolean first_gardener = false;
	static boolean has_called_for_gardener = false;
	static boolean sentDeathSignal = false;
	
	static void runGardener(RobotController rc) throws GameActionException{
		if(rc.readBroadcast(901) == 1){
			first_gardener = true;
		}
		while(true){
			if(first_gardener){
				runFirstGardener(rc);
			}
			else{
				runNotFirstGardener(rc);
			}
			Clock.yield();
		}
	}
	
	static void runFirstGardener(RobotController rc) throws GameActionException {
		GardenerALT.rc = rc;
		
		System.out.println("Checking about building trees");
		
		waterBestTree();
		RobotPlayer.updateEnemiesAndBroadcast();
		RobotPlayer.findShakableTrees();
		RobotPlayer.checkForStockpile();
		
		if(shouldBuildTree()){
			System.out.println("A tree is needed");
			boolean built = false;
			for (int i = 0; i < RobotPlayer.num_angles; i++) {
				Direction next_build;
				if (i % 2 == 0) {
					next_build = RobotPlayer.backward.rotateLeftRads(i / 2 * tree_angle_gradient);
				} else {
					next_build = RobotPlayer.backward.rotateRightRads((i + 1) / 2 * tree_angle_gradient);
				}
				if (rc.canPlantTree(next_build)) {
					built = true;
					rc.plantTree(next_build);
					System.out.println("Built a tree");
					break;
				}
			}
			if(!built && !built_lumberjack && rc.getTeamBullets() >= 50 && rc.isBuildReady()){
				System.out.println("Couldn't build a tree :( Need a lumberjack");
				if(rc.senseNearbyTrees(3, RobotPlayer.NEUTRAL).length > 0){
					need_lumberjack = true;
				}
			}
		}
		
		RobotType[] priority_build = getPriorityBuild();
		
		System.out.println("Checking priority builds");
		
		for(int j = 0; j < priority_build.length; j++){
			if(priority_build[j] == null){
				continue;
			}
			for(int i = 0; i < RobotPlayer.num_angles; i++){
				Direction left = RobotPlayer.forward.rotateLeftRads(gardener_angle_gradient * i);
				Direction right = RobotPlayer.forward.rotateRightRads(gardener_angle_gradient * i);
				if(rc.canBuildRobot(priority_build[j], left)){
					rc.buildRobot(priority_build[j], left);
					if(priority_build[j] == RobotType.LUMBERJACK){
						built_lumberjack = true;
					}
					System.out.println("Built a " + priority_build[j]);
					rc.broadcast(900 + RobotPlayer.typeToInt(priority_build[j]), rc.readBroadcast(900 + RobotPlayer.typeToInt(priority_build[j])) + 1);
					break;
				}
				if(rc.canBuildRobot(priority_build[j], right)){
					rc.buildRobot(priority_build[j], right);
					if(priority_build[j] == RobotType.LUMBERJACK){
						built_lumberjack = true;
					}
					System.out.println("Built a " + priority_build[j]);
					rc.broadcast(900 + RobotPlayer.typeToInt(priority_build[j]), rc.readBroadcast(900 + RobotPlayer.typeToInt(priority_build[j])) + 1);
					break;
				}
			}
		}
		
		if(rc.getRoundNum() < 25){
			System.out.println("Moving away from scary enemies");
			// Not planting trees yet, find a place to settle in as far from enemy as possible		
			for(int i = 0; i < RobotPlayer.num_angles / 4; i++){
				Direction left = RobotPlayer.backward.rotateLeftRads(gardener_angle_gradient * i);
				Direction right = RobotPlayer.backward.rotateRightRads(gardener_angle_gradient * i);
				if(rc.canMove(left) && RobotPlayer.distFromEdgeAtLeast(3.5f)){
					rc.move(left);
					break;
				}
				if(rc.canMove(right) && RobotPlayer.distFromEdgeAtLeast(3.5f)){
					rc.move(right);
					break;
				}
			}
			System.out.println("Moved away from scary enemies");
		}
		else{
			System.out.println("I'm home");
			// Reached place to stop
			reached_target = true;
			target = rc.getLocation();
			if(rc.readBroadcastFloat(1000) == 0){
				rc.broadcastFloat(1000, target.x);
			}
			if(rc.readBroadcastFloat(1001) == 0){
				rc.broadcastFloat(1001, target.y);
			}
			System.out.println("Broadcasting new targets");
			for(int i = 0; i < 6; i++){
				MapLocation potential_loc = rc.getLocation().add(RobotPlayer.forward.rotateLeftDegrees(60), 6.99f);
				if(isGoodBuildingLoc(potential_loc)){
					rc.broadcastFloat(1002, potential_loc.x);
					rc.broadcastFloat(1003, potential_loc.y);
				}
			}
		}
		
		if(!sentDeathSignal && rc.getHealth() < 8) {
			sentDeathSignal = true;
			rc.broadcast(900 + RobotPlayer.typeToInt(rc.getType()), rc.readBroadcast(900 + RobotPlayer.typeToInt(rc.getType())) - 1);
		}
		System.out.println("Ending my turn");
	}
	
	static void runNotFirstGardener(RobotController rc) throws GameActionException{
		GardenerALT.rc = rc;
		
		waterBestTree();
		RobotPlayer.updateEnemiesAndBroadcast();
		RobotPlayer.findShakableTrees();
		RobotPlayer.checkForStockpile();
		
		System.out.println("Getting target...");
		
		if(!reached_target && getBestGardenerLocation().x != RobotPlayer.INVALID_LOCATION.x){
			System.out.println("Target has been found");
			target = getBestGardenerLocation();	
		}
		
		if(target == null || rc.getLocation().distanceTo(target) < 0.5){
			System.out.println("Reached target");
			reached_target = true;
		}
		
		System.out.println("Checking priority builds");
		RobotType[] priority_build = getPriorityBuild();
		
		for(int j = 0; j < priority_build.length; j++){
			if(priority_build[j] == null){
				continue;
			}
			for(int i = 0; i < RobotPlayer.num_angles; i++){
				Direction left = RobotPlayer.forward.rotateLeftRads(gardener_angle_gradient * i);
				Direction right = RobotPlayer.forward.rotateRightRads(gardener_angle_gradient * i);
				if(rc.canBuildRobot(priority_build[j], left)){
					rc.buildRobot(priority_build[j], left);
					if(priority_build[j] == RobotType.LUMBERJACK){
						built_lumberjack = true;
					}
				}
				if(rc.canBuildRobot(priority_build[j], right)){
					rc.buildRobot(priority_build[j], right);
					if(priority_build[j] == RobotType.LUMBERJACK){
						built_lumberjack = true;
					}
				}
			}
		}
		
		if(shouldBuildTree()){
			System.out.println("A tree is needed");
			boolean built = false;
			for (int i = 0; i < RobotPlayer.num_angles; i++) {
				Direction next_build;
				if (i % 2 == 0) {
					next_build = RobotPlayer.backward.rotateLeftRads(i / 2 * tree_angle_gradient);
				} else {
					next_build = RobotPlayer.backward.rotateRightRads((i + 1) / 2 * tree_angle_gradient);
				}
				if (rc.canPlantTree(next_build)) {
					built = true;
					rc.plantTree(next_build);
					System.out.println("Built a tree");
					break;
				}
			}
			if(!built && !built_lumberjack && rc.getTeamBullets() >= 50 && rc.isBuildReady()){
				System.out.println("Can't build a tree :( Need a lumberjack");
				if(rc.senseNearbyTrees(3, RobotPlayer.NEUTRAL).length > 0){
					need_lumberjack = true;
				}
			}
		}
		
		if(!reached_target){
			System.out.println("Moving towards target at " + target);
			Direction to_target = rc.getLocation().directionTo(target);
			for(int i = 0; i < RobotPlayer.num_angles / 2; i++){
				Direction left = to_target.rotateLeftRads(gardener_angle_gradient * i);
				Direction right = to_target.rotateRightRads(gardener_angle_gradient * i);
				if(rc.canMove(left) && rc.getLocation().add(left, 0.5f).distanceTo(target) < rc.getLocation().distanceTo(target)){
					rc.move(left);
					break;
				}
				if(rc.canMove(right) && rc.getLocation().add(right, 0.5f).distanceTo(target) < rc.getLocation().distanceTo(target)){
					rc.move(right);
					break;
				}
			}
			if(!rc.hasMoved() && !built_lumberjack){
				need_lumberjack = true;
			}
		}
		else{
			System.out.println("Broadcasting new targets");
			for(int i = 0; i < 6; i++){
				MapLocation potential_loc = rc.getLocation().add(RobotPlayer.forward.rotateLeftDegrees(60 * i), 6.99f);
				if(isGoodBuildingLoc(potential_loc)){
					rc.broadcastFloat(1002, potential_loc.x);
					rc.broadcastFloat(1003, potential_loc.y);
				}
			}
		}
		if(!sentDeathSignal && rc.getHealth() < 8) {
			sentDeathSignal = true;
			rc.broadcast(900 + RobotPlayer.typeToInt(rc.getType()), rc.readBroadcast(900 + RobotPlayer.typeToInt(rc.getType())) - 1);
		}
	}
	
	static boolean isGoodBuildingLoc(MapLocation loc) throws GameActionException{
		if(!rc.onTheMap(loc)){
			return false;
		}
		if(rc.senseNearbyRobots(loc, -1, RobotPlayer.ENEMY).length > 0){
			return false;
		}
		if(rc.senseNearbyTrees(loc, 1.0f, RobotPlayer.NEUTRAL).length > 0){
			return false;
		}
		return true;
	}
	
	static MapLocation getBestGardenerLocation() throws GameActionException{
		
		if(rc.readBroadcastFloat(1002) != 0 && rc.readBroadcastFloat(1003) != 0){
			return new MapLocation(rc.readBroadcastFloat(1002), rc.readBroadcastFloat(1003));
		}
		else{
			return RobotPlayer.INVALID_LOCATION;
		}
	}
	
	static boolean shouldBuildTree() throws GameActionException{
		if(!reached_target){
			return false;
		}
		if(getPriorityBuild()[0] != null){
			return false;
		}
		int count = 0;
		for(int i = 0; i < 6; i++){
			Direction next_build = RobotPlayer.backward.rotateLeftRads(i * tree_angle_gradient);
            MapLocation check = rc.getLocation().add(next_build, 2);
			if(!rc.isCircleOccupiedExceptByThisRobot(check, 1) && rc.onTheMap(check, 1)){
				count++;
			}
		}
		if(count < 2){
			patience++;
			if(patience >= 25 && !has_called_for_gardener){
				callForGardener();
				has_called_for_gardener = true;
			}
			return false;
		}
		return true;
	}
	
	static void callForGardener() throws GameActionException{
		System.out.println("Calling for gardener");
		rc.broadcast(7, 1);
	}
	
	static RobotType[] getPriorityBuild() throws GameActionException{
		int archon_ct = rc.readBroadcast(900);
		int gardener_ct = rc.readBroadcast(901);
		int lumberjack_ct = rc.readBroadcast(902);
		int scout_ct = rc.readBroadcast(903);
		int soldier_ct = rc.readBroadcast(904);
		int tank_ct = rc.readBroadcast(905);
		int army_size = scout_ct + soldier_ct + tank_ct;
		int tree_ct = rc.getTreeCount();
		
		if(rc.getRoundNum() < 25){
			if(rc.senseNearbyTrees(3, RobotPlayer.NEUTRAL).length > 0 && !built_lumberjack){
				return new RobotType[]{RobotType.TANK, RobotType.LUMBERJACK, RobotType.SOLDIER};
			}
			else{
				return new RobotType[]{RobotType.TANK, RobotType.SOLDIER, RobotType.LUMBERJACK};
			}
		}
		
		if(tree_ct * RobotPlayer.getArmyToTreeRatio() < army_size){
			return new RobotType[]{null, RobotType.TANK, RobotType.SOLDIER, RobotType.LUMBERJACK};
		}
		if(army_size >= 2 && scout_ct < 1){
			return new RobotType[]{RobotType.SCOUT, RobotType.TANK, RobotType.SOLDIER, RobotType.LUMBERJACK};
		}
		if(need_lumberjack && !built_lumberjack){
			return new RobotType[]{RobotType.LUMBERJACK, RobotType.TANK, RobotType.SOLDIER};
		}
		
		return new RobotType[]{RobotType.TANK, RobotType.SOLDIER, RobotType.LUMBERJACK};
	}
	
	static void waterBestTree() throws GameActionException{
		TreeInfo[] friendly_trees = rc.senseNearbyTrees(-1, RobotPlayer.FRIEND);
		TreeInfo best_tree = null;
		for(int i = 0; i < friendly_trees.length; i++){
			if(!rc.canWater(friendly_trees[i].getID())){
				continue;
			}
			if(best_tree == null || friendly_trees[i].getHealth() < best_tree.getHealth()){
				best_tree = friendly_trees[i];
			}
		}
		if(best_tree != null){
			rc.water(best_tree.getID());
		}
	}
}
	
	