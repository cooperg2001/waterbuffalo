===============================
Version History for soldierrushV2
===============================


VERSION 2.0.0

1/25/2017 04:20 zobsniuk

Changes:

	2nd Aggresive Soldier-based AI
	*Synthesizes last soldierrush(V1) version on github with one mwabbitt posted on fb group
        *Fixed final bugs in bugpathing (this version gives no errors! finally!)
        *does NOT implement statuses for CombatUnit (yet?) (you'll have to explain to me what is useful about the statuses)
	*IMPORTANT: Removed canMove checks from get_best_location since we are now bugPathing when we can't move to the target
		-this makes it run notceably better

