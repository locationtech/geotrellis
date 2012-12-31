---
layout: overview-large
title: Creating Collections From Scratch

disqus: true

partof: collections
num: 16
languages: [ja]
---

You have syntax `List(1, 2, 3)` to create a list of three integers and `Map('A' -> 1, 'C' -> 2)` to create a map with two bindings. This is actually a universal feature of Scala collections. You can take any collection name and follow it by a list of elements in parentheses. The result will be a new collection with the given elements. Here are some more examples:

    Traversable()             // An empty traversable object
    List()                    // The empty list
    List(1.0, 2.0)            // A list with elements 1.0, 2.0
    Vector(1.0, 2.0)          // A vector with elements 1.0, 2.0
    Iterator(1, 2, 3)         // An iterator returning three integers.
    Set(dog, cat, bird)       // A set of three animals
    HashSet(dog, cat, bird)   // A hash set of the same animals
    Map(a -> 7, 'b' -> 0)     // A map from characters to integers

"Under the covers" each of the above lines is a call to the `apply` method of some object. For instance, the third line above expands to

    List.apply(1.0, 2.0)

So this is a call to the `apply` method of the companion object of the `List` class. That method takes an arbitrary number of arguments an constructs a list from them. Every collection class in the Scala library has a companion object with such an `apply` method. It does not matter whether the collection class represents a concrete implementation, like `List`, or `Stream` or `Vector`, do, or whether it is an abstract base class such as `Seq`, `Set` or `Traversable`. In the latter case, calling apply will produce some default implementation of the abstract base class. Examples:

    scala> List(1, 2, 3)
    res17: List[Int] = List(1, 2, 3)
    scala> Traversable(1, 2, 3)
    res18: Traversable[Int] = List(1, 2, 3)
    scala> mutable.Traversable(1, 2, 3)
    res19: scala.collection.mutable.Traversable[Int] = ArrayBuffer(1, 2, 3)

Besides `apply`, every collection companion object also defines a member `empty`, which returns an empty collection. So instead of `List()` you could write `List.empty`, instead of `Map()`, `Map.empty`, and so on.

Descendants of `Seq` classes provide also other factory operations in their companion objects. These are summarized in the following table. In short, there's

* `concat`, which concatenates an arbitrary number of traversables together,
* `fill` and `tabulate`, which generate single or multi-dimensional sequences of given dimensions initialized by some expression or tabulating function,
* `range`, which generates integer sequences with some constant step length, and
* `iterate`, which generates the sequence resulting from repeated application of a function to a start element.

### Factory Methods for Sequences

| WHAT IT IS  	  	        | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  `S.empty`         	    | The empty sequence. |
|  `S(x, y, z)`      	    | A sequence consisting of elements `x, y, z`. |
|  `S.concat(xs, ys, zs)`   | The sequence obtained by concatenating the elements of `xs, ys, zs`. |
|  `S.fill(n){e}`      	    | A sequence of length `n` where each element is computed by expression `e`. |
|  `S.fill(m, n){e}`        | A sequence of sequences of dimension `m×n` where each element is computed by expression `e`. (exists also in higher dimensions). |
|  `S.tabulate(n){f}`       | A sequence of length `n` where the element at each index i is computed by `f(i)`. |
|  `S.tabulate(m, n){f}`    | A sequence of sequences of dimension `m×n` where the element at each index `(i, j)` is computed by `f(i, j)`. (exists also in higher dimensions). |
|  `S.range(start, end)`    | The sequence of integers `start` ... `end-1`. |
|  `S.range(start, end, step)`| The sequence of integers starting with `start` and progressing by `step` increments up to, and excluding, the `end` value. |
|  `S.iterate(x, n)(f)`     | The sequence of length `n` with elements `x`, `f(x)`, `f(f(x))`, ... |