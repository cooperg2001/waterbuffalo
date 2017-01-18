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
