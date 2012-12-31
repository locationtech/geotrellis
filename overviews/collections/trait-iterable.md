---
layout: overview-large
title: Trait Iterable

disqus: true

partof: collections
num: 4
languages: [ja]
---

The next trait from the top in the collections hierarchy is `Iterable`. All methods in this trait are defined in terms of an an abstract method, `iterator`, which yields the collection's elements one by one. The `foreach` method from trait `Traversable` is implemented in `Iterable` in terms of `iterator`. Here is the actual implementation:

    def foreach[U](f: Elem => U): Unit = {
      val it = iterator
      while (it.hasNext) f(it.next())
    } 

Quite a few subclasses of `Iterable` override this standard implementation of foreach in `Iterable`, because they can provide a more efficient implementation. Remember that `foreach` is the basis of the implementation of all operations in `Traversable`, so its performance matters.

Two more methods exist in `Iterable` that return iterators: `grouped` and `sliding`. These iterators, however, do not return single elements but whole subsequences of elements of the original collection. The maximal size of these subsequences is given as an argument to these methods. The `grouped` method returns its elements in "chunked" increments, where `sliding` yields a sliding "window" over the elements. The difference between the two should become clear by looking at the following REPL interaction:

    scala> val xs = List(1, 2, 3, 4, 5)
    xs: List[Int] = List(1, 2, 3, 4, 5)
    scala> val git = xs grouped 3
    git: Iterator[List[Int]] = non-empty iterator
    scala> git.next()
    res3: List[Int] = List(1, 2, 3)
    scala> git.next()
    res4: List[Int] = List(4, 5)
    scala> val sit = xs sliding 3
    sit: Iterator[List[Int]] = non-empty iterator
    scala> sit.next()
    res5: List[Int] = List(1, 2, 3)
    scala> sit.next()
    res6: List[Int] = List(2, 3, 4)
    scala> sit.next()
    res7: List[Int] = List(3, 4, 5)

Trait `Iterable` also adds some other methods to `Traversable` that can be implemented efficiently only if an iterator is available. They are summarized in the following table.

### Operations in Trait Iterable ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Abstract Method:**     |						     |
|  `xs.iterator`	    |An `iterator` that yields every element in `xs`, in the same order as `foreach` traverses elements.|
|  **Other Iterators:**     |						     |
|  `xs grouped size`   	    |An iterator that yields fixed-sized "chunks" of this collection.|
|  `xs sliding size`   	    |An iterator that yields a sliding fixed-sized window of elements in this collection.|
|  **Subcollections:** 	    |						     |
|  `xs takeRight n`	    |A collection consisting of the last `n` elements of `xs` (or, some arbitrary `n` elements, if no order is defined).|
|  `xs dropRight n`	    |The rest of the collection except `xs takeRight n`.|
|  **Zippers:** 	    |						     |
|  `xs zip ys`	    	    |An iterable of pairs of corresponding elements from `xs` and `ys`.|
|  `xs zipAll (ys, x, y)`   |An iterable of pairs of corresponding elements from `xs` and `ys`, where the shorter sequence is extended to match the longer one by appending elements `x` or `y`.|
|  `xs.zipWithIndex`	    |An iterable of pairs of elements from `xs` with their indices.|
|  **Comparison:** 	    |						     |
|  `xs sameElements ys`	    |A test whether `xs` and `ys` contain the same elements in the same order|

In the inheritance hierarchy below Iterable you find three traits: [Seq](http://www.scala-lang.org/docu/files/collections-api/collections_5.html), [Set](http://www.scala-lang.org/docu/files/collections-api/collections_7.html), and [Map](http://www.scala-lang.org/docu/files/collections-api/collections_10.html). A common aspect of these three traits is that they all implement the [PartialFunction](http://www.scala-lang.org/api/current/scala/PartialFunction.html) trait with its `apply` and `isDefinedAt` methods. However, the way each trait implements [PartialFunction](http://www.scala-lang.org/api/current/scala/PartialFunction.html) differs.

For sequences, `apply` is positional indexing, where elements are always numbered from `0`. That is, `Seq(1, 2, 3)(1)` gives `2`. For sets, `apply` is a membership test. For instance, `Set('a', 'b', 'c')('b')` gives `true` whereas `Set()('a')` gives `false`. Finally for maps, `apply` is a selection. For instance, `Map('a' -> 1, 'b' -> 10, 'c' -> 100)('b')` gives `10`.

In the following, we will explain each of the three kinds of collections in more detail.
