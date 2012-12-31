---
layout: overview-large
title: Concurrent Tries

disqus: true

partof: parallel-collections
num: 4
---

Most concurrent data structures do not guarantee consistent
traversal if the the data structure is modified during traversal.
This is, in fact, the case with most mutable collections, too.
Concurrent tries are special in the sense that they allow you to modify
the trie being traversed itself. The modifications are only visible in the
subsequent traversal. This holds both for sequential concurrent tries and their
parallel counterparts. The only difference between the two is that the
former traverses its elements sequentially, whereas the latter does so in
parallel.

This is a nice property that allows you to write some algorithms more
easily. Typically, these are algorithms that process a dataset of elements
iteratively, in which different elements need a different number of
iterations to be processed.

The following example computes the square roots of a set of numbers. Each iteration
iteratively updates the square root value. Numbers whose square roots converged
are removed from the map.

    case class Entry(num: Double) {
      var sqrt = num
    }
	
    val length = 50000
	
	// prepare the list
    val entries = (1 until length) map { num => Entry(num.toDouble) }
    val results = ParTrieMap()
    for (e <- entries) results += ((e.num, e))
    
	// compute square roots
    while (results.nonEmpty) {
      for ((num, e) <- results) {
        val nsqrt = 0.5 * (e.sqrt + e.num / e.sqrt)
        if (math.abs(nsqrt - e.sqrt) < 0.01) {
          results.remove(num)
        } else e.sqrt = nsqrt
      }
    }

Note that in the above Babylonian method square root computation
(\[[3][3]\]) some numbers may converge much faster than the others. For
this reason, we want to remove them from `results` so that only those
elements that need to be worked on are traversed.

Another example is the breadth-first search algorithm, which iteratively expands the nodefront
until either it finds some path to the target or there are no more
nodes to expand. We define a node on a 2D map as a tuple of
`Int`s. We define the `map` as a 2D array of booleans which denote is
the respective slot occupied or not. We then declare 2 concurrent trie
maps-- `open` which contains all the nodes which have to be
expanded (the nodefront), and `closed` which contains all the nodes which have already
been expanded. We want to start the search from the corners of the map and
find a path to the center of the map-- we initialize the `open` map
with appropriate nodes. Then we iteratively expand all the nodes in
the `open` map in parallel until there are no more nodes to expand.
Each time a node is expanded, it is removed from the `open` map and
placed in the `closed` map.
Once done, we output the path from the target to the initial node.
	
	val length = 1000
	
	// define the Node type
    type Node = (Int, Int);
    type Parent = (Int, Int);
    
	// operations on the Node type
    def up(n: Node) = (n._1, n._2 - 1);
    def down(n: Node) = (n._1, n._2 + 1);
    def left(n: Node) = (n._1 - 1, n._2);
    def right(n: Node) = (n._1 + 1, n._2);
    
    // create a map and a target
    val target = (length / 2, length / 2);
    val map = Array.tabulate(length, length)((x, y) => (x % 3) != 0 || (y % 3) != 0 || (x, y) == target)
    def onMap(n: Node) = n._1 >= 0 && n._1 < length && n._2 >= 0 && n._2 < length
    
    // open list - the nodefront
    // closed list - nodes already processed
    val open = ParTrieMap[Node, Parent]()
    val closed = ParTrieMap[Node, Parent]()
    
    // add a couple of starting positions
    open((0, 0)) = null
    open((length - 1, length - 1)) = null
    open((0, length - 1)) = null
    open((length - 1, 0)) = null
 
    // greedy bfs path search
    while (open.nonEmpty && !open.contains(target)) {
      for ((node, parent) <- open) {
        def expand(next: Node) {
          if (onMap(next) && map(next._1)(next._2) && !closed.contains(next) && !open.contains(next)) {
            open(next) = node
          }
        }
        expand(up(node))
        expand(down(node))
        expand(left(node))
        expand(right(node))
        closed(node) = parent
        open.remove(node)
      }
    }
	
    // print path
    var pathnode = open(target)
    while (closed.contains(pathnode)) {
      print(pathnode + "->")
      pathnode = closed(pathnode)
    }
    println()

There is a Game of Life example on GitHub which uses Ctries to
selectively simulate only those parts of the Game of Life automaton which
are currently active \[[4][4]\].
It also includes a Swing-based visualization of the Game of Life simulation,
in which you can observe how tweaking the parameters affects performance.

The concurrent tries also support a linearizable, lock-free, constant
time `snapshot` operation. This operation creates a new concurrent
trie with all the elements at a specific point in time, thus in effect
capturing the state of the trie at a specific point.
The `snapshot` operation merely creates
a new root for the concurrent trie. Subsequent updates lazily rebuild the part of
the concurrent trie relevant to the update and leave the rest of the concurrent trie
intact. First of all, this means that the snapshot operation by itself is not expensive
since it does not copy the elements. Second, since the copy-on-write optimization copies
only parts of the concurrent trie, subsequent modifications scale horizontally.
The `readOnlySnapshot` method is slightly more efficient than the
`snapshot` method, but returns a read-only map which cannot be
modified. Concurrent tries also support a linearizable, constant-time
`clear` operation based on the snapshot mechanism.
To learn more about how concurrent tries and snapshots work, see \[[1][1]\] and \[[2][2]\].

The iterators for concurrent tries are based on snapshots. Before the iterator
object gets created, a snapshot of the concurrent trie is taken, so the iterator
only traverses the elements in the trie at the time at which the snapshot was created.
Naturally, the iterators use the read-only snapshot.

The `size` operation is also based on the snapshot. A straightforward implementation, the `size`
call would just create an iterator (i.e. a snapshot) and traverse the elements to count them.
Every call to `size` would thus require time linear in the number of elements. However, concurrent
tries have been optimized to cache sizes of their different parts, thus reducing the complexity
of the `size` method to amortized logarithmic time. In effect, this means that after calling
`size` once, subsequent calls to `size` will require a minimum amount of work, typically recomputing
the size only for those branches of the trie which have been modified since the last `size` call.
Additionally, size computation for parallel concurrent tries is performed in parallel.




## References

1. [Cache-Aware Lock-Free Concurrent Hash Tries][1]
2. [Concurrent Tries with Efficient Non-Blocking Snapshots][2]
3. [Methods of computing square roots][3]
4. [Game of Life simulation][4]

  [1]: http://infoscience.epfl.ch/record/166908/files/ctries-techreport.pdf "Ctries-techreport"
  [2]: http://lampwww.epfl.ch/~prokopec/ctries-snapshot.pdf "Ctries-snapshot"
  [3]: http://en.wikipedia.org/wiki/Methods_of_computing_square_roots#Babylonian_method "babylonian-method"
  [4]: https://github.com/axel22/ScalaDays2012-TrieMap "game-of-life-ctries"
