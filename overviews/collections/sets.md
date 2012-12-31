---
layout: overview-large
title: Sets

disqus: true

partof: collections
num: 6
languages: [ja]
---

`Set`s are `Iterable`s that contain no duplicate elements. The operations on sets are summarized in the following table for general sets and in the table after that for mutable sets. They fall into the following categories:

* **Tests** `contains`, `apply`, `subsetOf`. The `contains` method asks whether a set contains a given element. The `apply` method for a set is the same as `contains`, so `set(elem)` is the same as `set contains elem`. That means sets can also be used as test functions that return true for the elements they contain. 

For example


    val fruit = Set("apple", "orange", "peach", "banana")
    fruit: scala.collection.immutable.Set[java.lang.String] = 
    Set(apple, orange, peach, banana)
    scala> fruit("peach")
    res0: Boolean = true
    scala> fruit("potato")
    res1: Boolean = false


* **Additions** `+` and `++`, which add one or more elements to a set, yielding a new set.
* **Removals** `-`, `--`, which remove one or more elements from a set, yielding a new set.
* **Set operations** for union, intersection, and set difference. Each of these operations exists in two forms: alphabetic and symbolic. The alphabetic versions are `intersect`, `union`, and `diff`, whereas the symbolic versions are `&`, `|`, and `&~`. In fact, the `++` that Set inherits from `Traversable` can be seen as yet another alias of `union` or `|`, except that `++` takes a `Traversable` argument whereas `union` and `|` take sets.

### Operations in Class Set ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Tests:**               |						     |
|  `xs contains x`  	    |Tests whether `x` is an element of `xs`.        |
|  `xs(x)`        	    |Same as `xs contains x`.                        |
|  `xs subsetOf ys`  	    |Tests whether `xs` is a subset of `ys`.         |
|  **Additions:**           |						     |
|  `xs + x`                 |The set containing all elements of `xs` as well as `x`.|
|  `xs + (x, y, z)`         |The set containing all elements of `xs` as well as the given additional elements.|
|  `xs ++ ys`  	            |The set containing all elements of `xs` as well as all elements of `ys`.|
|  **Tests:**               |						     |
|  `xs - x`  	            |The set containing all elements of `xs` except `x`.|
|  `xs - (x, y, z)`         |The set containing all elements of `xs` except the given elements.|
|  `xs -- ys`  	            |The set containing all elements of `xs` except the elements of `ys`.|
|  `xs.empty`  	            |An empty set of the same class as `xs`.         |
|  **Binary Operations:**   |						     |
|  `xs & ys`  	            |The set intersection of `xs` and `ys`.          |
|  `xs intersect ys`        |Same as `xs & ys`.                              |
|  <code>xs &#124; ys</code>  	            |The set union of `xs` and `ys`.                 |
|  `xs union ys`  	    |Same as <code>xs &#124; ys</code>.                              |
|  `xs &~ ys`  	            |The set difference of `xs` and `ys`.            |
|  `xs diff ys`  	    |Same as `xs &~ ys`.                             |

Mutable sets offer in addition methods to add, remove, or update elements, which are summarized in below.

### Operations in Class mutable.Set ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Additions:**           |						     |
|  `xs += x`  	            |Adds element `x` to set `xs` as a side effect and returns `xs` itself.|
|  `xs += (x, y, z)`        |Adds the given elements to set `xs` as a side effect and returns `xs` itself.|
|  `xs ++= ys`  	    |Adds all elements in `ys` to set `xs` as a side effect and returns `xs` itself.|
|  `xs add x`  	            |Adds element `x` to `xs` and returns `true` if `x` was not previously contained in the set, `false` if it was.|
|  **Removals:**            |						     |
|  `xs -= x`  	            |Removes element `x` from set `xs` as a side effect and returns `xs` itself.|
|  `xs -= (x, y, z)`  	    |Removes the given elements from set `xs` as a side effect and returns `xs` itself.|
|  `xs --= ys`  	    |Removes all elements in `ys` from set `xs` as a side effect and returns `xs` itself.|
|  `xs remove x`  	    |Removes element `x` from `xs` and returns `true` if `x` was previously contained in the set, `false` if it was not.|
|  `xs retain p`  	    |Keeps only those elements in `xs` that satisfy predicate `p`.|
|  `xs.clear()`  	    |Removes all elements from `xs`.|
|  **Update:**              |						     |
|  `xs(x) = b`  	    |(or, written out, `xs.update(x, b)`). If boolean argument `b` is `true`, adds `x` to `xs`, otherwise removes `x` from `xs`.|
|  **Cloning:**             |						     |
|  `xs.clone`  	            |A new mutable set with the same elements as `xs`.|

Just like an immutable set, a mutable set offers the `+` and `++` operations for element additions and the `-` and `--` operations for element removals. But these are less often used for mutable sets since they involve copying the set. As a more efficient alternative, mutable sets offer the update methods `+=` and `-=`. The operation `s += elem` adds `elem` to the set `s` as a side effect, and returns the mutated set as a result. Likewise, `s -= elem` removes `elem` from the set, and returns the mutated set as a result. Besides `+=` and `-=` there are also the bulk operations `++=` and `--=` which add or remove all elements of a traversable or an iterator.

The choice of the method names `+=` and `-=` means that very similar code can work with either mutable or immutable sets. Consider first the following REPL dialogue which uses an immutable set `s`:

    scala> var s = Set(1, 2, 3)
    s: scala.collection.immutable.Set[Int] = Set(1, 2, 3)
    scala> s += 4
    scala> s -= 2
    scala> s
    res2: scala.collection.immutable.Set[Int] = Set(1, 3, 4)

We used `+=` and `-=` on a `var` of type `immutable.Set`. A statement such as `s += 4` is an abbreviation for `s = s + 4`. So this invokes the addition method `+` on the set `s` and then assigns the result back to the `s` variable. Consider now an analogous interaction with a mutable set.


    scala> val s = collection.mutable.Set(1, 2, 3)
    s: scala.collection.mutable.Set[Int] = Set(1, 2, 3)
    scala> s += 4
    res3: s.type = Set(1, 4, 2, 3)
    scala> s -= 2
    res4: s.type = Set(1, 4, 3)

The end effect is very similar to the previous interaction; we start with a `Set(1, 2, 3)` end end up with a `Set(1, 3, 4)`. However, even though the statements look the same as before, they do something different. `s += 4` now invokes the `+=` method on the mutable set value `s`, changing the set in place. Likewise, `s -= 2` now invokes the `-=` method on the same set.

Comparing the two interactions shows an important principle. You often can replace a mutable collection stored in a `val` by an immutable collection stored in a `var`, and _vice versa_. This works at least as long as there are no alias references to the collection through which one can observe whether it was updated in place or whether a new collection was created.

Mutable sets also provide add and remove as variants of `+=` and `-=`. The difference is that `add` and `remove` return a Boolean result indicating whether the operation had an effect on the set.

The current default implementation of a mutable set uses a hashtable to store the set's elements. The default implementation of an immutable set uses a representation that adapts to the number of elements of the set. An empty set is represented by just a singleton object. Sets of sizes up to four are represented by a single object that stores all elements as fields. Beyond that size, immutable sets are implemented as [hash tries](#hash-tries).

A consequence of these representation choices is that, for sets of small sizes (say up to 4), immutable sets are usually more compact and also more efficient than mutable sets. So, if you expect the size of a set to be small, try making it immutable.

Two subtraits of sets are `SortedSet` and `BitSet`.

### Sorted Sets ###

A [SortedSet](http://www.scala-lang.org/api/current/scala/collection/SortedSet.html) is a set that produces its elements (using `iterator` or `foreach`) in a given ordering (which can be freely chosen at the time the set is created). The default representation of a [SortedSet](http://www.scala-lang.org/api/current/scala/collection/SortedSet.html) is an ordered binary tree which maintains the invariant that all elements in the left subtree of a node are smaller than all elements in the right subtree. That way, a simple in order traversal can return all tree elements in increasing order. Scala's class [immutable.TreeSet](http://www.scala-lang.org/api/current/scala/collection/immutable/TreeSet.html) uses a _red-black_ tree implementation to maintain this ordering invariant and at the same time keep the tree _balanced_-- meaning that all paths from the root of the tree to a leaf have lengths that differ only by at most one element.

To create an empty [TreeSet](http://www.scala-lang.org/api/current/scala/collection/immutable/TreeSet.html), you could first specify the desired ordering:

    scala> val myOrdering = Ordering.fromLessThan[String](_ > _)
    myOrdering: scala.math.Ordering[String] = ...

Then, to create an empty tree set with that ordering, use:

    scala> TreeSet.empty(myOrdering)
    res1: scala.collection.immutable.TreeSet[String] = TreeSet()

Or you can leave out the ordering argument but give an element type or the empty set. In that case, the default ordering on the element type will be used.

    scala> TreeSet.empty[String]
    res2: scala.collection.immutable.TreeSet[String] = TreeSet()

If you create new sets from a tree-set (for instance by concatenation or filtering) they will keep the same ordering as the original set. For instance,

scala> res2 + ("one", "two", "three", "four")
res3: scala.collection.immutable.TreeSet[String] = TreeSet(four, one, three, two)

Sorted sets also support ranges of elements. For instance, the `range` method returns all elements from a starting element up to, but excluding, and end element. Or, the `from` method returns all elements greater or equal than a starting element in the set's ordering. The result of calls to both methods is again a sorted set. Examples:

    scala> res3 range ("one", "two")
    res4: scala.collection.immutable.TreeSet[String] = TreeSet(one, three)
    scala> res3 from "three"
    res5: scala.collection.immutable.TreeSet[String] = TreeSet(three, two)


### Bitsets ###

Bitsets are sets of non-negative integer elements that are implemented in one or more words of packed bits. The internal representation of a [BitSet](http://www.scala-lang.org/api/current/scala/collection/BitSet.html) uses an array of `Long`s. The first `Long` covers elements from 0 to 63, the second from 64 to 127, and so on (Immutable bitsets of elements in the range of 0 to 127 optimize the array away and store the bits directly in a one or two `Long` fields.) For every  `Long`, each of its 64 bits is set to 1 if the corresponding element is contained in the set, and is unset otherwise. It follows that the size of a bitset depends on the largest integer that's stored in it. If `N` is that largest integer, then the size of the set is `N/64` `Long` words, or `N/8` bytes, plus a small number of extra bytes for status information.

Bitsets are hence more compact than other sets if they contain many small elements. Another advantage of bitsets is that operations such as membership test with `contains`, or element addition and removal with `+=` and `-=` are all extremely efficient.

