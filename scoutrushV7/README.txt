===============================
Version History for scoutrushV7
===============================

VERSION 7.2.1
1/19/2017 19:14 alexkatz
Changes:
	ARCHON.JAVA
	* Archons now broadcast locations of all enemy archons
	GARDENER.JAVA
	* Changed lumberjacks to spawn very quickly
	* Tweaked soldiers to spawn slightly later
		- condition is now numSoldiers < numScouts from numSoldiers < numScouts + 1
	* Tweaked scouts so that they would only spawn in early game (<400) and midgame (>700)

	ROBOTPLAYER.JAVA
	* Bugfix for shotWillHit method

VERSION 7.2
1/19/2017 17:09 mwabbitt, alexkatz, zobsniuk, r31415
Changes:
	COMBATUNIT.JAVA
	* Slightly more conservative with pentad shots
	GARDENER.JAVA
	* Lumberjack building tweaked to occur if ARMY - 6 > 4 * jacks.
	LUMBERJACK.JAVA
	* Lumberjacks now have three statuses: chasing, retreating, and chopping wood.
		- Retreating takes highest priority, chasing second, chopping wood third.
	* Lumberjacks now heuristically chop trees outward from spawn
		- They prioritize chopping the trees closest to spawn they can see
	ROBOTPLAYER.JAVA
	* Under get_best_location, scouts ignore archons until Rnd 300

VERSION 7.1
1/18/2017 15:05 mwabbitt
Changes:
	GARDENER.JAVA
	* Code for finding unoccupied adjacent spots cleaned to use 33% of bytecode
	* Lumberjack building tweaked to occur if ARMY - 6 > 7 * jacks.
	LUMBERJACK.JAVA
	* Lumberjacks no longer stall - they are fully absorbed in chopping trees.
	ROBOTPLAYER.JAVA
	* RobotPlayer helper functions now have spec placeholders. Fill in at your own convenience.

Bugs:
	* Scouts run into their own bullets if moving towards an enemy.

To change:
	* Lumberjacks should engage enemy units if they approach



VERSION 7.0
?/??/???? ??:?? alexkatz
Seventh cohesive aggressive AI in development.



Changes:

	* Scout targeting system fixed

Bugs:
	* Scouts don't shoot gardeners occasionally		
	* Lumberjack movement glitches
		- They wander incessantly
		- Occasionally they stand still for no apparent reason