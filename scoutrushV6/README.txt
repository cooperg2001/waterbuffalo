===============================
Version History for scoutrushV6
===============================

VERSION 6.2
1/19/2017 17:09 mwabbitt, alexkatz, zobsniuk, r31415
Changes:
	COMBATUNIT.JAVA
	* Slightly more conservative with pentad shots
	GARDENER.JAVA
	* Lumberjack building tweaked to occur if ARMY- 6 > 4 * jacks
	LUMBERJACK.JAVA
	* Lumberjacks now have three statuses: chasing, retreating, and chopping wood.
		- Retreating takes highest priority, chasing second, chopping wood third.
	* Lumberjacks now heuristically chop trees outward from spawn
		- They prioritize chopping the trees closest to spawn they can see
	ROBOTPLAYER.JAVA
	* Under get_best_location, scouts ignore archons until Rnd 300

VERSION 6.1
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
	* Scouts don't shoot gardeners occasionally

To change:
	* Lumberjacks should engage enemy units if they approach



VERSION 6.0
Sixth cohesive aggressive AI in development.


?/??/???? ??:?? ????
Changes:

	* Adds rudimentary support for lumberjacks.
		- Currently they move like combat units
		- We should make them stay in a more centralized area
	* Attempts to implement the scout rush strategy
		- Gardener construction conditions tweaked to ARMY > 5 * gardeners - 5 as opposed to > 5 * gardeners
		- Builds two gardeners very quickly for optimal (?) early bullet collection
		- The first scouts are not built as quickly as they could be
Bugs:
	* Scouts don't shoot gardeners occasionally		
	* Lumberjack movement glitches
		- They wander incessantly
		- Occasionally they stand still for no apparent reason