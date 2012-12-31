---
layout: overview-large
title: Equality

disqus: true

partof: collections
num: 13
languages: [ja]
---

The collection libraries have a uniform approach to equality and hashing. The idea is, first, to divide collections into sets, maps, and sequences. Collections in different categories are always unequal. For instance, `Set(1, 2, 3)` is unequal to `List(1, 2, 3)` even though they contain the same elements. On the other hand, within the same category, collections are equal if and only if they have the same elements (for sequences: the same elements in the same order). For example, `List(1, 2, 3) == Vector(1, 2, 3)`, and `HashSet(1, 2) == TreeSet(2, 1)`.

It does not matter for the equality check whether a collection is mutable or immutable. For a mutable collection one simply considers its current elements at the time the equality test is performed. This means that a mutable collection might be equal to different collections at different times, depending what elements are added or removed. This is a potential trap when using a mutable collection as a key in a hashmap. Example:

    scala> import collection.mutable.{HashMap, ArrayBuffer}
    import collection.mutable.{HashMap, ArrayBuffer}
    scala> val buf = ArrayBuffer(1, 2, 3)
    buf: scala.collection.mutable.ArrayBuffer[Int] = 
    ArrayBuffer(1, 2, 3)
    scala> val map = HashMap(buf -> 3)
    map: scala.collection.mutable.HashMap[scala.collection.
    mutable.ArrayBuffer[Int],Int] = Map((ArrayBuffer(1, 2, 3),3))
    scala> map(buf)
    res13: Int = 3
    scala> buf(0) += 1
    scala> map(buf)
    java.util.NoSuchElementException: key not found: 
    ArrayBuffer(2, 2, 3) 

In this example, the selection in the last line will most likely fail because the hash-code of the array `xs` has changed in the second-to-last line. Therefore, the hash-code-based lookup will look at a different place than the one where `xs` was stored.
