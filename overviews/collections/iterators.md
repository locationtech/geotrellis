---
layout: overview-large
title: Iterators

disqus: true

partof: collections
num: 15
languages: [ja]
---

An iterator is not a collection, but rather a way to access the elements of a collection one by one. The two basic operations on an iterator `it` are `next` and `hasNext`. A call to `it.next()` will return the next element of the iterator and advance the state of the iterator. Calling `next` again on the same iterator will then yield the element one beyond the one returned previously. If there are no more elements to return, a call to `next` will throw a `NoSuchElementException`. You can find out whether there are more elements to return using [Iterator](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/Iterator.html)'s `hasNext` method.

The most straightforward way to "step through" all the elements returned by an iterator `it` uses a while-loop:

    while (it.hasNext) 
      println(it.next())

Iterators in Scala also provide analogues of most of the methods that you find in the `Traversable`, `Iterable` and `Seq` classes. For instance, they provide a `foreach` method which executes a given procedure on each element returned by an iterator. Using `foreach`, the loop above could be abbreviated to:

    it foreach println

As always, for-expressions can be used as an alternate syntax for expressions involving `foreach`, `map`, `withFilter`, and `flatMap`, so yet another way to print all elements returned by an iterator would be:

    for (elem <- it) println(elem)

There's an important difference between the foreach method on iterators and the same method on traversable collections: When called to an iterator, `foreach` will leave the iterator at its end when it is done. So calling `next` again on the same iterator will fail with a `NoSuchElementException`. By contrast, when called on on a collection, `foreach` leaves the number of elements in the collection unchanged (unless the passed function adds to removes elements, but this is discouraged, because it may lead to surprising results).

The other operations that Iterator has in common with `Traversable` have the same property. For instance, iterators provide a `map` method, which returns a new iterator:

    scala> val it = Iterator("a", "number", "of", "words")
    it: Iterator[java.lang.String] = non-empty iterator
    scala> it.map(_.length)
    res1: Iterator[Int] = non-empty iterator
    scala> res1 foreach println
    1
    6
    2
    5
    scala> it.next()
    java.util.NoSuchElementException: next on empty iterator

As you can see, after the call to `it.map`, the `it` iterator has advanced to its end.

Another example is the `dropWhile` method, which can be used to find the first elements of an iterator that has a certain property. For instance, to find the first word in the iterator above that has at least two characters you could write:

    scala> val it = Iterator("a", "number", "of", "words")
    it: Iterator[java.lang.String] = non-empty iterator
    scala> it dropWhile (_.length < 2)
    res4: Iterator[java.lang.String] = non-empty iterator
    scala> it.next()
    res5: java.lang.String = number

Note again that `it` has changed by the call to `dropWhile`: it now points to the second word "number" in the list. In fact, `it` and the result `res4` returned by `dropWhile` will return exactly the same sequence of elements.

There is only one standard operation which allows to re-use the same iterator: The call

    val (it1, it2) = it.duplicate

gives you _two_ iterators which each return exactly the same elements as the iterator `it`. The two iterators work independently; advancing one does not affect the other. By contrast the original iterator `it` is advanced to its end by `duplicate` and is thus rendered unusable.

In summary, iterators behave like collections _if one never accesses an iterator again after invoking a method on it_. The Scala collection libraries make this explicit with an abstraction [TraversableOnce](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/TraversableOnce.html), which is a common superclass of [Traversable](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/Traversable.html) and [Iterator](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/Iterator.html). As the name implies, `TraversableOnce` objects can be traversed using `foreach` but the state of that object after the traversal is not specified. If the `TraversableOnce` object is in fact an `Iterator`, it will be at its end after the traversal, but if it is a `Traversable`, it will still exist as before. A common use case of `TraversableOnce` is as an argument type for methods that can take either an iterator or a traversable as argument. An example is the appending method `++` in class `Traversable`. It takes a `TraversableOnce` parameter, so you can append elements coming from either an iterator or a traversable collection.

All operations on iterators are summarized below.

### Operations in class Iterator

| WHAT IT IS  	  	        | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Abstract Methods:**    |						         |
|  `it.next()`      	    | Returns next element on iterator and advances past it. |
|  `it.hasNext`  	        | Returns `true` if `it` can return another element. |
|  **Variations:**          |						         |
|  `it.buffered`      	    | A buffered iterator returning all elements of `it`. |
|  `it grouped size`      	| An iterator that yields the elements elements returned by `it` in fixed-sized sequence "chunks". |
|  `xs sliding size`      	| An iterator that yields the elements elements returned by `it` in sequences representing a sliding fixed-sized window. |
|  **Duplication:**         |						         |
|  `it.duplicate`           | A pair of iterators that each independently return all elements of `it`. |
|  **Additions:**           |						         |
|  `it ++ jt`               | An iterator returning all elements returned by iterator `it`, followed by all elements returned by iterator `jt`. |
|  `it padTo (len, x)`      | The iterator that first returns all elements of `it` and then follows that by copies of `x` until length `len` elements are returned overall. |
|  **Maps:**                |						         |
|  `it map f`               | The iterator obtained from applying the function `f` to every element returned from `it`. |
|  `it flatMap f`           | The iterator obtained from applying the iterator-valued function f to every element in `it` and appending the results. |
|  `it collect f`           | The iterator obtained from applying the partial function `f` to every element in `it` for which it is defined and collecting the results. |
|  **Conversions:**         |						         |
|  `it.toArray`             | Collects the elements returned by `it` in an array. |
|  `it.toList`              | Collects the elements returned by `it` in a list. |
|  `it.toIterable`          | Collects the elements returned by `it` in an iterable. |
|  `it.toSeq`               | Collects the elements returned by `it` in a sequence. |
|  `it.toIndexedSeq`        | Collects the elements returned by `it` in an indexed sequence. |
|  `it.toStream`            | Collects the elements returned by `it` in a stream. |
|  `it.toSet`               | Collects the elements returned by `it` in a set. |
|  `it.toMap`               | Collects the key/value pairs returned by `it` in a map. |
|  **Coying:**              |						         |
|  `it copyToBuffer buf`    | Copies all elements returned by `it` to buffer `buf`. |
|  `it copyToArray(arr, s, n)`| Copies at most `n` elements returned by `it` to array `arr` starting at index `s`. The last two arguments are optional. |
|  **Size Info:**           |						         |
|  `it.isEmpty`             | Test whether the iterator is empty (opposite of `hasNext`). |
|  `it.nonEmpty`            | Test whether the collection contains elements (alias of `hasNext`). |
|  `it.size`                | The number of elements returned by `it`. Note: `it` will be at its end after this operation! |
|  `it.length`              | Same as `it.size`. |
|  `it.hasDefiniteSize`     | Returns `true` if `it` is known to return finitely many elements (by default the same as `isEmpty`). |
|  **Element Retrieval Index Search:**|						         |
|  `it find p`              | An option containing the first element returned by `it` that satisfies `p`, or `None` is no element qualifies. Note: The iterator advances to after the element, or, if none is found, to the end. |
|  `it indexOf x`           | The index of the first element returned by `it` that equals `x`. Note: The iterator advances past the position of this element. |
|  `it indexWhere p`        | The index of the first element returned by `it` that satisfies `p`. Note: The iterator advances past the position of this element. |
|  **Subiterators:**        |						         |
|  `it take n`              | An iterator returning of the first `n` elements of `it`. Note: it will advance to the position after the `n`'th element, or to its end, if it contains less than `n` elements. |
|  `it drop n`              | The iterator that starts with the `(n+1)`'th element of `it`. Note: `it` will advance to the same position. |
|  `it slice (m,n)`         | The iterator that returns a slice of the elements returned from it, starting with the `m`'th element and ending before the `n`'th element. |
|  `it takeWhile p`         | An iterator returning elements from `it` as long as condition `p` is true. |
|  `it dropWhile p`         | An iterator skipping elements from `it` as long as condition `p` is `true`, and returning the remainder. |
|  `it filter p`            | An iterator returning all elements from `it` that satisfy the condition `p`. |
|  `it withFilter p`        | Same as `it` filter `p`. Needed so that iterators can be used in for-expressions. |
|  `it filterNot p`         | An iterator returning all elements from `it` that do not satisfy the condition `p`. |
|  **Subdivisions:**        |						         |
|  `it partition p`         | Splits `it` into a pair of two iterators; one returning all elements from `it` that satisfy the predicate `p`, the other returning all elements from `it` that do not. |
|  **Element Conditions:**  |						         |
|  `it forall p`            | A boolean indicating whether the predicate p holds for all elements returned by `it`. |
|  `it exists p`            | A boolean indicating whether the predicate p holds for some element in `it`. |
|  `it count p`             | The number of elements in `it` that satisfy the predicate `p`. |
|  **Folds:**               |						         |
|  `(z /: it)(op)`          | Apply binary operation `op` between successive elements returned by `it`, going left to right and starting with `z`. |
|  `(it :\ z)(op)`          | Apply binary operation `op` between successive elements returned by `it`, going right to left and starting with `z`. |
|  `it.foldLeft(z)(op)`     | Same as `(z /: it)(op)`. |
|  `it.foldRight(z)(op)`    | Same as `(it :\ z)(op)`. |
|  `it reduceLeft op`       | Apply binary operation `op` between successive elements returned by non-empty iterator `it`, going left to right. |
|  `it reduceRight op`      | Apply binary operation `op` between successive elements returned by non-empty iterator `it`, going right to left. |
|  **Specific Folds:**      |						         |
|  `it.sum`                 | The sum of the numeric element values returned by iterator `it`. |
|  `it.product`             | The product of the numeric element values returned by iterator `it`. |
|  `it.min`                 | The minimum of the ordered element values returned by iterator `it`. |
|  `it.max`                 | The maximum of the ordered element values returned by iterator `it`. |
|  **Zippers:**             |						         |
|  `it zip jt`              | An iterator of pairs of corresponding elements returned from iterators `it` and `jt`. |
|  `it zipAll (jt, x, y)`   | An iterator of pairs of corresponding elements returned from iterators `it` and `jt`, where the shorter iterator is extended to match the longer one by appending elements `x` or `y`. |
|  `it.zipWithIndex`        | An iterator of pairs of elements returned from `it` with their indices. |
|  **Update:**              |						         |
|  `it patch (i, jt, r)`    | The iterator resulting from `it` by replacing `r` elements starting with `i` by the patch iterator `jt`. |
|  **Comparison:**          |						         |
|  `it sameElements jt`     | A test whether iterators it and `jt` return the same elements in the same order. Note: At least one of `it` and `jt` will be at its end after this operation. |
|  **Strings:**             |						         |
|  `it addString (b, start, sep, end)`| Adds a string to `StringBuilder` `b` which shows all elements returned by `it` between separators `sep` enclosed in strings `start` and `end`. `start`, `sep`, `end` are all optional. |
|  `it mkString (start, sep, end)` | Converts the collection to a string which shows all elements returned by `it` between separators `sep` enclosed in strings `start` and `end`. `start`, `sep`, `end` are all optional. |

### Buffered iterators

Sometimes you want an iterator that can "look ahead", so that you can inspect the next element to be returned without advancing past that element. Consider for instance, the task to skip leading empty strings from an iterator that returns a sequence of strings. You might be tempted to write the following


    def skipEmptyWordsNOT(it: Iterator[String]) =
      while (it.next().isEmpty) {}
  
But looking at this code more closely, it's clear that this is wrong: The code will indeed skip leading empty strings, but it will also advance `it` past the first non-empty string!

The solution to this problem is to use a buffered iterator. Class [BufferedIterator](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/BufferedIterator.html) is a subclass of [Iterator](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/Iterator.html), which provides one extra method, `head`. Calling `head` on a buffered iterator will return its first element but will not advance the iterator. Using a buffered iterator, skipping empty words can be written as follows.

    def skipEmptyWords(it: BufferedIterator[String]) =
      while (it.head.isEmpty) { it.next() }

Every iterator can be converted to a buffered iterator by calling its `buffered` method. Here's an example:

    scala> val it = Iterator(1, 2, 3, 4)
    it: Iterator[Int] = non-empty iterator
    scala> val bit = it.buffered
    bit: java.lang.Object with scala.collection.
      BufferedIterator[Int] = non-empty iterator
    scala> bit.head
    res10: Int = 1
    scala> bit.next()
    res11: Int = 1
    scala> bit.next()
    res11: Int = 2

Note that calling `head` on the buffered iterator `bit` does not advance it. Therefore, the subsequent call `bit.next()` returns the same value as `bit.head`.
