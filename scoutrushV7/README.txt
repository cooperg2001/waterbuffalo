===============================
Version History for scoutrushV7
===============================

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