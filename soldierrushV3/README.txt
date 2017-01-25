===============================

Version History for soldierrushV2
===============================



VERSION 3.0.0


1/25/2017 14:51 mwabbitt

	3rd Aggresive Soldier-based AI
	

Changes:

	ROBOTPLAYER.JAVA
	* All bugpathing logic is now inside this file - to bugpath, just call one function in the unit class file
	* Removed all instances of "MapLocation == MapLocation", replaced with "MapLocation.equals(MapLocation)

	COMBATUNIT.JAVA
	* Split into SCOUT.JAVA and HEAVYINFANTRY.JAVA

	HEAVYINFANTRY.JAVA
	* If soldiers can shoot something, they don't bugpath until they stop shooting.
	* IF they're not adjcent to trees but are adjacent to robots, they start thinking about resetting the bugpath algorithm. This prevents robots squaredancing. There are edge cases where robots still do silly things, but I'm pretty sure these are benign.

	LUMBERJACK.JAVA
	* Lumberjacks now cut down enemy trees to get to enemy units.	
	* Lumberjacks now do not strike if they are within striking distance of a friendly. Instead, they chop down trees.
	* Lumberjacks recognize when turtles occur, and go and engage the enemy.