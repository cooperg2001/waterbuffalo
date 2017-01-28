=============================
Version History for BuffaloV0
=============================

VERSION 0.1
1/28/2017 12:13 mwabbitt

Changes:
	ARCHON.JAVA
	* No longer moves with get_best_location

	GARDENER.JAVA
	* Removes magic numbers in calculation of should_build_tree

	ROBOTPLAYER.JAVA
	* Revamps bugpathing
	* Fix center calculation
	* All helper function names changed to camelcase with no underscores
	
	COMBATSTRATEGY.JAVA
	* shotWillHit is now overloaded: Second input can either be a MapLocation or a RobotInfo.


VERSION 0.0
1/27/2017 3:00 zobsniuk
The beginning of a revamp of the combat strategy code

Changes:
	* Creates CombatStrategy.java