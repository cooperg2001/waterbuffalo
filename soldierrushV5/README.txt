=================================
Version History for soldierrushV5
=================================

VERSION 5.0
1/27/2017 03:07 zobsniuk

Changes:
	General
	— Smarter economy, only hiring gardeners when every gardener has only one spot left (for units)
	— The boolean (stored as 0/1) for shouldHireGardener is stored in broadcast channel 7, with channel 8 holding the roundNum as metadata of the signal
	
	LUMBERJACK.JAVA
	— There was a bug in V4 where the braces were wrong, a for loop was encasing too much

	ROBOTPLAYER.JAVA
	—checkForStockpile now donates a bunch if bullets>350 since we can potentially turtle

	GARDENER.JAVA
	—Uses much smaller bytecode algorithm for testing whether we can build a tree while leaving a spot open (600 compared to 6000)