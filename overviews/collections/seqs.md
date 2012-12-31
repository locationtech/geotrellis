---
layout: overview-large
title: The sequence traits Seq, IndexedSeq, and LinearSeq

disqus: true

partof: collections
num: 5
languages: [ja]
---

The [Seq](http://www.scala-lang.org/api/current/scala/collection/Seq.html) trait represents sequences. A sequence is a kind of iterable that has a `length` and whose elements have fixed index positions, starting from `0`.

The operations on sequences, summarized in the table below, fall into the following categories:

* **Indexing and length** operations `apply`, `isDefinedAt`, `length`, `indices`, and `lengthCompare`. For a `Seq`, the `apply` operation means indexing; hence a sequence of type `Seq[T]` is a partial function that takes an `Int` argument (an index) and which yields a sequence element of type `T`. In other words `Seq[T]` extends `PartialFunction[Int, T]`. The elements of a sequence are indexed from zero up to the `length` of the sequence minus one. The `length` method on sequences is an alias of the `size` method of general collections. The `lengthCompare` method allows you to compare the lengths of two sequences even if one of the sequences has infinite length.
* **Index search operations** `indexOf`, `lastIndexOf`, `indexofSlice`, `lastIndexOfSlice`, `indexWhere`, `lastIndexWhere`, `segmentLength`, `prefixLength`, which return the index of an element equal to a given value or matching some predicate.
* **Addition operations** `+:`, `:+`, `padTo`, which return new sequences obtained by adding elements at the front or the end of a sequence.
* **Update operations** `updated`, `patch`, which return a new sequence obtained by replacing some elements of the original sequence.
* **Sorting operations** `sorted`, `sortWith`, `sortBy`, which sort sequence elements according to various criteria.
* **Reversal operations** `reverse`, `reverseIterator`, `reverseMap`, which yield or process sequence elements in reverse order.
* **Comparisons** `startsWith`, `endsWith`, `contains`, `containsSlice`, `corresponds`, which relate two sequences or search an element in a sequence.
* **Multiset** operations `intersect`, `diff`, `union`, `distinct`, which perform set-like operations on the elements of two sequences or remove duplicates.

If a sequence is mutable, it offers in addition a side-effecting `update` method, which lets sequence elements be updated. As always in Scala, syntax like `seq(idx) = elem` is just a shorthand for `seq.update(idx, elem)`, so `update` gives convenient assignment syntax for free. Note the difference between `update` and `updated`. `update` changes a sequence element in place, and is only available for mutable sequences. `updated` is available for all sequences and always returns a new sequence instead of modifying the original.

### Operations in Class Seq ###

| WHAT IT IS  	  	    | WHAT IT DOES				     |
| ------       	       	    | ------					     |
|  **Indexing and Length:** |						     |
|  `xs(i)`    	  	    |(or, written out, `xs apply i`). The element of `xs` at index `i`.|
|  `xs isDefinedAt i`	    |Tests whether `i` is contained in `xs.indices`.|
|  `xs.length`	    	    |The length of the sequence (same as `size`).|
|  `xs.lengthCompare ys`    |Returns `-1` if `xs` is shorter than `ys`, `+1` if it is longer, and `0` is they have the same length. Works even if one if the sequences is infinite.|
|  `xs.indices`	     	    |The index range of `xs`, extending from `0` to `xs.length - 1`.|
|  **Index Search:**        |						     |
|  `xs indexOf x`   	    |The index of the first element in `xs` equal to `x` (several variants exist).|
|  `xs lastIndexOf x`       |The index of the last element in `xs` equal to `x` (several variants exist).|
|  `xs indexOfSlice ys`     |The first index of `xs` such that successive elements starting from that index form the sequence `ys`.|
|  `xs lastIndexOfSlice ys` |The last index of `xs` such that successive elements starting from that index form the sequence `ys`.|
|  `xs indexWhere p`   	    |The index of the first element in xs that satisfies `p` (several variants exist).|
|  `xs segmentLength (p, i)`|The length of the longest uninterrupted segment of elements in `xs`, starting with `xs(i)`, that all satisfy the predicate `p`.|
|  `xs prefixLength p` 	    |The length of the longest prefix of elements in `xs` that all satisfy the predicate `p`.|
|  **Additions:** 	    |						     |
|  `x +: xs` 	    	    |A new sequence that consists of `x` prepended to `xs`.|
|  `xs :+ x` 	    	    |A new sequence that consists of `x` appended to `xs`.|
|  `xs padTo (len, x)` 	    |The sequence resulting from appending the value `x` to `xs` until length `len` is reached.|
|  **Updates:** 	    |						     |
|  `xs patch (i, ys, r)`    |The sequence resulting from replacing `r` elements of `xs` starting with `i` by the patch `ys`.|
|  `xs updated (i, x)`      |A copy of `xs` with the element at index `i` replaced by `x`.|
|  `xs(i) = x`	    	    |(or, written out, `xs.update(i, x)`, only available for `mutable.Seq`s). Changes the element of `xs` at index `i` to `x`.|
|  **Sorting:** 	    |						     |
|  `xs.sorted`	            |A new sequence obtained by sorting the elements of `xs` using the standard ordering of the element type of `xs`.|
|  `xs sortWith lt`	    |A new sequence obtained by sorting the elements of `xs` using `lt` as comparison operation.|
|  `xs sortBy f`	    |A new sequence obtained by sorting the elements of `xs`. Comparison between two elements proceeds by mapping the function `f` over both and comparing the results.|
|  **Reversals:** 	    |						     |
|  `xs.reverse`	            |A sequence with the elements of `xs` in reverse order.|
|  `xs.reverseIterator`	    |An iterator yielding all the elements of `xs` in reverse order.|
|  `xs reverseMap f`	    |A sequence obtained by mapping `f` over the elements of `xs` in reverse order.|
|  **Comparisons:** 	    |						     |
|  `xs startsWith ys`	    |Tests whether `xs` starts with sequence `ys` (several variants exist).|
|  `xs endsWith ys`	    |Tests whether `xs` ends with sequence `ys` (several variants exist).|
|  `xs contains x`	    |Tests whether `xs` has an element equal to `x`.|
|  `xs containsSlice ys`    |Tests whether `xs` has a contiguous subsequence equal to `ys`.|
|  `(xs corresponds ys)(p)` |Tests whether corresponding elements of `xs` and `ys` satisfy the binary predicate `p`.|
|  **Multiset Operations:** |						     |
|  `xs intersect ys`	    |The multi-set intersection of sequences `xs` and `ys` that preserves the order of elements in `xs`.|
|  `xs diff ys`	    	    |The multi-set difference of sequences `xs` and `ys` that preserves the order of elements in `xs`.|
|  `xs union ys`	    |Multiset union; same as `xs ++ ys`.|
|  `xs.distinct`	    |A subsequence of `xs` that contains no duplicated element.|

Trait [Seq](http://www.scala-lang.org/api/current/scala/collection/Seq.html) has two subtraits [LinearSeq](http://www.scala-lang.org/api/current/scala/collection/IndexedSeq.html), and [IndexedSeq](http://www.scala-lang.org/api/current/scala/collection/IndexedSeq.html). These do not add any new operations, but each offers different performance characteristics: A linear sequence has efficient `head` and `tail` operations, whereas an indexed sequence has efficient `apply`, `length`, and (if mutable) `update` operations. Frequently used linear sequences are `scala.collection.immutable.List` and `scala.collection.immutable.Stream`. Frequently used indexed sequences are `scala.Array` and `scala.collection.mutable.ArrayBuffer`. The `Vector` class provides an interesting compromise between indexed and linear access. It has both effectively constant time indexing overhead and constant time linear access overhead. Because of this, vectors are a good foundation for mixed access patterns where both indexed and linear accesses are used. You'll learn more on vectors [later](#vectors).

### Buffers ###

An important sub-category of mutable sequences is `Buffer`s. They allow not only updates of existing elements but also element insertions, element removals, and efficient additions of new elements at the end of the buffer. The principal new methods supported by a buffer are `+=` and `++=` for element addition at the end, `+=:` and `++=:` for addition at the front, `insert` and `insertAll` for element insertions, as well as `remove` and `-=` for element removal. These operations are summarized in the following table.

Two often used implementations of buffers are `ListBuffer` and `ArrayBuffer`.  As the name implies, a `ListBuffer` is backed by a `List`, and supports efficient conversion of its elements to a `List`, whereas an `ArrayBuffer` is backed by an array, and can be quickly converted into one.

#### Operations in Class Buffer ####

| WHAT IT IS               | WHAT IT DOES|
| ------                   | ------                                                           |
|  **Additions:**          |                                                                  |
|  `buf += x`              |Appends element `x` to buffer, and returns `buf` itself as result.|
|  `buf += (x, y, z)`      |Appends given elements to buffer.|
|  `buf ++= xs`            |Appends all elements in `xs` to buffer.|
|  `x +=: buf`             |Prepends element `x` to buffer.|
|  `xs ++=: buf`           |Prepends all elements in `xs` to buffer.|
|  `buf insert (i, x)`     |Inserts element `x` at index `i` in buffer.|
|  `buf insertAll (i, xs)` |Inserts all elements in `xs` at index `i` in buffer.|
|  **Removals:**           |                                                                  |
|  `buf -= x`              |Removes element `x` from buffer.|
|  `buf remove i`          |Removes element at index `i` from buffer.|
|  `buf remove (i, n)`     |Removes `n` elements starting at index `i` from buffer.|
|  `buf trimStart n`       |Removes first `n` elements from buffer.|
|  `buf trimEnd n`         |Removes last `n` elements from buffer.|
|  `buf.clear()`           |Removes all elements from buffer.|
|  **Cloning:**            |                                                                  |
|  `buf.clone`             |A new buffer with the same elements as `buf`.|
