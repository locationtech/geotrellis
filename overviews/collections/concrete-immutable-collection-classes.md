---
layout: overview-large
title: Concrete Immutable Collection Classes

disqus: true

partof: collections
num: 8
languages: [ja]
---

Scala provides many concrete immutable collection classes for you to choose from. They differ in the traits they implement (maps, sets, sequences), whether they can be infinite, and the speed of various operations. Here are some of the most common immutable collection types used in Scala.

## Lists

A [List](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/List.html) is a finite immutable sequence. They provide constant-time access to their first element as well as the rest of the list, and they have a constant-time cons operation for adding a new element to the front of the list. Many other operations take linear time.

Lists have always been the workhorse for Scala programming, so not much needs to be said about them here. The major change in 2.8 is that the `List` class together with its subclass `::` and its subobject `Nil` is now defined in package `scala.collection.immutable`, where it logically belongs. There are still aliases for `List`, `Nil`, and `::` in the `scala` package, so from a user perspective, lists can be accessed as before.

Another change is that lists now integrate more closely into the collections framework, and are less of a special case than before. For instance all of the numerous methods that originally lived in the `List` companion object have been deprecated. They are replaced by the [uniform creation methods]({{ site.baseurl }}/overviews/collections/creating-collections-from-scratch.html) inherited by every collection.

## Streams

A [Stream](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Stream.html) is like a list except that its elements are computed lazily. Because of this, a stream can be infinitely long. Only those elements requested are computed. Otherwise, streams have the same performance characteristics as lists.

Whereas lists are constructed with the `::` operator, streams are constructed with the similar-looking `#::`. Here is a simple example of a stream containing the integers 1, 2, and 3:

    scala> val str = 1 #:: 2 #:: 3 #:: Stream.empty
    str: scala.collection.immutable.Stream[Int] = Stream(1, ?)

The head of this stream is 1, and the tail of it has 2 and 3. The tail is not printed here, though, because it hasn't been computed yet! Streams are specified to compute lazily, and the `toString` method of a stream is careful not to force any extra evaluation.

Below is a more complex example. It computes a stream that contains a Fibonacci sequence starting with the given two numbers. A Fibonacci sequence is one where each element is the sum of the previous two elements in the series.


    scala> def fibFrom(a: Int, b: Int): Stream[Int] = a #:: fibFrom(b, a + b)
    fibFrom: (a: Int,b: Int)Stream[Int]

This function is deceptively simple. The first element of the sequence is clearly `a`, and the rest of the sequence is the Fibonacci sequence starting with `b` followed by `a + b`. The tricky part is computing this sequence without causing an infinite recursion. If the function used `::` instead of `#::`, then every call to the function would result in another call, thus causing an infinite recursion. Since it uses `#::`, though, the right-hand side is not evaluated until it is requested.
Here are the first few elements of the Fibonacci sequence starting with two ones:

    scala> val fibs = fibFrom(1, 1).take(7)
    fibs: scala.collection.immutable.Stream[Int] = Stream(1, ?)
    scala> fibs.toList
    res9: List[Int] = List(1, 1, 2, 3, 5, 8, 11)

## Vectors

Lists are very efficient when the algorithm processing them is careful to only process their heads. Accessing, adding, and removing the head of a list takes only constant time, whereas accessing or modifying elements later in the list takes time linear in the depth into the list.

[Vector](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Vector.html) is a collection type (introduced in Scala 2.8) that addresses the inefficiency for random access on lists. Vectors allow accessing any element of the list in "effectively" constant time. It's a larger constant than for access to the head of a list or for reading an element of an array, but it's a constant nonetheless. As a result, algorithms using vectors do not have to be careful about accessing just the head of the sequence. They can access and modify elements at arbitrary locations, and thus they can be much more convenient to write.

Vectors are built and modified just like any other sequence.

    scala> val vec = scala.collection.immutable.Vector.empty
    vec: scala.collection.immutable.Vector[Nothing] = Vector()
    scala> val vec2 = vec :+ 1 :+ 2
    vec2: scala.collection.immutable.Vector[Int] = Vector(1, 2)
    scala> val vec3 = 100 +: vec2
    vec3: scala.collection.immutable.Vector[Int] = Vector(100, 1, 2)
    scala> vec3(0)
    res1: Int = 100

Vectors are represented as trees with a high branching factor (The branching factor of a tree or a graph is the number of children at each node). Every tree node contains up to 32 elements of the vector or contains up to 32 other tree nodes. Vectors with up to 32 elements can be represented in a single node. Vectors with up to `32 * 32 = 1024` elements can be represented with a single indirection. Two hops from the root of the tree to the final element node are sufficient for vectors with up to 2<sup>15</sup> elements, three hops for vectors with 2<sup>20</sup>, four hops for vectors with 2<sup>25</sup> elements and five hops for vectors with up to 2<sup>30</sup> elements. So for all vectors of reasonable size, an element selection involves up to 5 primitive array selections. This is what we meant when we wrote that element access is "effectively constant time".

Vectors are immutable, so you cannot change an element of a vector and still retain a new vector. However, with the `updated` method you can crate a new vector that differs from a given vector only in a single element:

    scala> val vec = Vector(1, 2, 3)
    vec: scala.collection.immutable.Vector[Int] = Vector(1, 2, 3)
    scala> vec updated (2, 4)
    res0: scala.collection.immutable.Vector[Int] = Vector(1, 2, 4)
    scala> vec
    res1: scala.collection.immutable.Vector[Int] = Vector(1, 2, 3)

As the last line above shows, a call to `updated` has no effect on the original vector `vec`. Like selection, functional vector updates are also "effectively constant time". Updating an element in the middle of a vector can be done by copying the node that contains the element, and every node that points to it, starting from the root of the tree. This means that a functional update creates between one and five nodes that each contain up to 32 elements or subtrees. This is certainly more expensive than an in-place update in a mutable array, but still a lot cheaper than copying the whole vector.

Because vectors strike a good balance between fast random selections and fast random functional updates, they are currently the default implementation of immutable indexed sequences:


    scala> collection.immutable.IndexedSeq(1, 2, 3)
    res2: scala.collection.immutable.IndexedSeq[Int] = Vector(1, 2, 3)

## Immutable stacks

If you need a last-in-first-out sequence, you can use a [Stack](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Stack.html). You push an element onto a stack with `push`, pop an element with `pop`, and peek at the top of the stack without removing it with `top`. All of these operations are constant time.

Here are some simple operations performed on a stack:


    scala> val stack = scala.collection.immutable.Stack.empty
    stack: scala.collection.immutable.Stack[Nothing] = Stack()
    scala> val hasOne = stack.push(1)
    hasOne: scala.collection.immutable.Stack[Int] = Stack(1)
    scala> stack
    stack: scala.collection.immutable.Stack[Nothing] = Stack()
    scala> hasOne.top
    res20: Int = 1
    scala> hasOne.pop
    res19: scala.collection.immutable.Stack[Int] = Stack()

Immutable stacks are used rarely in Scala programs because their functionality is subsumed by lists: A `push` on an immutable stack is the same as a `::` on a list and a `pop` on a stack is the same a `tail` on a list.

## Immutable Queues

A [Queue](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Queue.html) is just like a stack except that it is first-in-first-out rather than last-in-first-out.

Here's how you can create an empty immutable queue:

    scala> val empty = scala.collection.immutable.Queue[Int]()
    empty: scala.collection.immutable.Queue[Int] = Queue()

You can append an element to an immutable queue with `enqueue`:

    scala> val has1 = empty.enqueue(1)
    has1: scala.collection.immutable.Queue[Int] = Queue(1)

To append multiple elements to a queue, call `enqueue` with a collection as its argument:

    scala> val has123 = has1.enqueue(List(2, 3))
    has123: scala.collection.immutable.Queue[Int]
      = Queue(1, 2, 3)

To remove an element from the head of the queue, you use `dequeue`:

    scala> val (element, has23) = has123.dequeue
    element: Int = 1
    has23: scala.collection.immutable.Queue[Int] = Queue(2, 3)

Note that `dequeue` returns a pair consisting of the element removed and the rest of the queue.

## Ranges

A [Range](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/Range.html) is an ordered sequence of integers that are equally spaced apart. For example, "1, 2, 3," is a range, as is "5, 8, 11, 14." To create a range in Scala, use the predefined methods `to` and `by`.

    scala> 1 to 3
    res2: scala.collection.immutable.Range.Inclusive
      with scala.collection.immutable.Range.ByOne = Range(1, 2, 3)
    scala> 5 to 14 by 3
    res3: scala.collection.immutable.Range = Range(5, 8, 11, 14)

If you want to create a range that is exclusive of its upper limit, then use the convenience method `until` instead of `to`:

    scala> 1 until 3
    res2: scala.collection.immutable.Range.Inclusive
      with scala.collection.immutable.Range.ByOne = Range(1, 2)

Ranges are represented in constant space, because they can be defined by just three numbers: their start, their end, and the stepping value. Because of this representation, most operations on ranges are extremely fast.

## Hash Tries

Hash tries are a standard way to implement immutable sets and maps efficiently. They are supported by class [immutable.HashMap](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/HashMap.html). Their representation is similar to vectors in that they are also trees where every node has 32 elements or 32 subtrees. But the selection of these keys is now done based on hash code. For instance, to find a given key in a map, one first takes the hash code of the key. Then, the lowest 5 bits of the hash code are used to select the first subtree, followed by the next 5 bits and so on. The selection stops once all elements stored in a node have hash codes that differ from each other in the bits that are selected up to this level.

Hash tries strike a nice balance between reasonably fast lookups and reasonably efficient functional insertions (`+`) and deletions (`-`). That's why they underly Scala's default implementations of immutable maps and sets. In fact, Scala has a further optimization for immutable sets and maps that contain less than five elements. Sets and maps with one to four elements are stored as single objects that just contain the elements (or key/value pairs in the case of a map) as fields. The empty immutable set and the empty immutable map is in each case a single object - there's no need to duplicate storage for those because and empty immutable set or map will always stay empty.

## Red-Black Trees

Red-black trees are a form of balanced binary trees where some nodes are designated "red" and others designated "black." Like any balanced binary tree, operations on them reliably complete in time logarithmic to the size of the tree.

Scala provides implementations of immutable sets and maps that use a red-black tree internally. Access them under the names [TreeSet](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/TreeSet.html) and [TreeMap](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/TreeMap.html).


    scala> scala.collection.immutable.TreeSet.empty[Int]
    res11: scala.collection.immutable.TreeSet[Int] = TreeSet()
    scala> res11 + 1 + 3 + 3
    res12: scala.collection.immutable.TreeSet[Int] = TreeSet(1, 3)

Red black trees are the standard implementation of `SortedSet` in Scala, because they provide an efficient iterator that returns all elements in sorted order.

## Immutable BitSets

A [BitSet](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/BitSet.html) represents a collection of small integers as the bits of a larger integer. For example, the bit set containing 3, 2, and 0 would be represented as the integer 1101 in binary, which is 13 in decimal.

Internally, bit sets use an array of 64-bit `Long`s. The first `Long` in the array is for integers 0 through 63, the second is for 64 through 127, and so on. Thus, bit sets are very compact so long as the largest integer in the set is less than a few hundred or so.

Operations on bit sets are very fast. Testing for inclusion takes constant time. Adding an item to the set takes time proportional to the number of `Long`s in the bit set's array, which is typically a small number. Here are some simple examples of the use of a bit set:

    scala> val bits = scala.collection.immutable.BitSet.empty
    bits: scala.collection.immutable.BitSet = BitSet()
    scala> val moreBits = bits + 3 + 4 + 4
    moreBits: scala.collection.immutable.BitSet = BitSet(3, 4)
    scala> moreBits(3)
    res26: Boolean = true
    scala> moreBits(0)
    res27: Boolean = false

## List Maps

A [ListMap](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/immutable/ListMap.html) represents a map as a linked list of key-value pairs. In general, operations on a list map might have to iterate through the entire list. Thus, operations on a list map take time linear in the size of the map. In fact there is little usage for list maps in Scala because standard immutable maps are almost always faster. The only possible difference is if the map is for some reason constructed in such a way that the first elements in the list are selected much more often than the other elements.

    scala> val map = scala.collection.immutable.ListMap(1->"one", 2->"two")
    map: scala.collection.immutable.ListMap[Int,java.lang.String] = 
       Map(1 -> one, 2 -> two)
    scala> map(2)
    res30: String = "two"






















