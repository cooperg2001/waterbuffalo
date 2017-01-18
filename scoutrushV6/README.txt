============================================

Organized code of scoutrush V6
============================================


Sixth cohesive aggressive AI in development.


Changes:

* Adds rudimentary support for lumberjacks.
	- Currently they move like combat units
	- We should make them stay in a more centralized area
* Attempts to implement the scout rush strategy
	- Gardener construction conditions tweaked to ARMY > 5 * gardeners - 5 as opposed to > 5 * gardeners
	- Builds two gardeners very quickly for optimal (?) early bullet collection
	- The first scouts are not built as quickly as they could be



Bugs:
* Scouts follow Archons before the 300th turn
	- So they are focused on it but don't shoot, wasting time
* Scouts shoot while they're in trees
	- Occasionally they target units and then sit in trees, but then just use their bullets on the tree
* Lumberjack movement glitches
	- They wander incessantly
	- Occasionally they stand still for no apparent reason
