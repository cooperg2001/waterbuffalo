package buffalov0;
import battlecode.common.*;

public class Scout{
	static void runScout(RobotController rc) throws GameActionException{
		TreeInfo target_tree = null;
		Direction rand = RobotPlayer.randomDirection();
		
		while(true){
			try{
				RobotPlayer.findShakableTrees();
				RobotPlayer.updateEnemiesAndBroadcast();
				TreeInfo[] trees = rc.senseNearbyTrees(-1, RobotPlayer.NEUTRAL);
				if(target_tree != null){
					target_tree = rc.senseTree(target_tree.getID());
				}
				if(target_tree != null && target_tree.getContainedBullets() == 0){
					System.out.println("Target tree has no bullets");
					target_tree = null;
				}
				for(int i = 0; i < trees.length; i++){
					if(trees[i].getContainedBullets() == 0){
						continue;
					}
					if(target_tree == null || rc.getLocation().distanceTo(trees[i].getLocation()) < rc.getLocation().distanceTo(target_tree.getLocation())){
						target_tree = trees[i];
					}
				}
				
				if(target_tree != null){
					System.out.println("Going towards tree at " + target_tree.getLocation());
					Direction towards_tree = rc.getLocation().directionTo(target_tree.getLocation());
					for(int i = 0; i < RobotPlayer.num_angles; i++){
						Direction left_i = towards_tree.rotateLeftDegrees(360.0f/RobotPlayer.num_angles * i);
						Direction right_i = towards_tree.rotateRightDegrees(360.0f/RobotPlayer.num_angles * i);
						MapLocation left = rc.getLocation().add(left_i, 1.25f);
						MapLocation right = rc.getLocation().add(right_i, 1.25f);
						if(rc.canMove(left)){
							rc.move(left);
							break;
						}
						if(rc.canMove(right)){
							rc.move(right);
							break;
						}
					}
				}
				
				if(!rc.hasMoved()){
					System.out.println("Can't move towards target tree, moving randomly instead");
					target_tree = null;
					int trials = 0;
					while(!rc.canMove(rand) && trials < 10){
						rand = RobotPlayer.randomDirection();
					}
					if(rc.canMove(rand)){
						rc.move(rand);
					}
				}
				
				Clock.yield();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	
	}

}
