---
layout: overview-large
title: Conversions Between Java and Scala Collections

disqus: true

partof: collections
num: 17
languages: [ja]
---

Like Scala, Java also has a rich collections library. There are many similarities between the two. For instance, both libraries know iterators, iterables, sets, maps, and sequences. But there are also important differences. In particular, the Scala libraries put much more emphasis on immutable collections, and provide many more operations that transform a collection into a new one.

Sometimes you might need to pass from one collection framework to the other. For instance, you might want to access to an existing Java collection, as if it was a Scala collection. Or you might want to pass one of Scala's collections to a Java method that expects its Java counterpart. It is quite easy to do this, because Scala offers implicit conversions between all the major collection types in the [JavaConversions](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/JavaConversions$.html) object. In particular, you will find bidirectional conversions between the following types.


    Iterator               <=>     java.util.Iterator
    Iterator               <=>     java.util.Enumeration
    Iterable               <=>     java.lang.Iterable
    Iterable               <=>     java.util.Collection
    mutable.Buffer         <=>     java.util.List
    mutable.Set            <=>     java.util.Set
    mutable.Map            <=>     java.util.Map
    mutable.ConcurrentMap  <=>     java.util.concurrent.ConcurrentMap

To enable these conversions, simply import them from the [JavaConversions](http://www.scala-lang.org/api/{{ site.scala-version }}/scala/collection/JavaConversions$.html) object:

    scala> import collection.JavaConversions._
    import collection.JavaConversions._

You have now automatic conversions between Scala collections and their corresponding Java collections.

    scala> import collection.mutable._
    import collection.mutable._
    scala> val jul: java.util.List[Int] = ArrayBuffer(1, 2, 3)
    jul: java.util.List[Int] = [1, 2, 3]
    scala> val buf: Seq[Int] = jul
    buf: scala.collection.mutable.Seq[Int] = ArrayBuffer(1, 2, 3)
    scala> val m: java.util.Map[String, Int] = HashMap("abc" -> 1, "hello" -> 2)
    m: java.util.Map[String,Int] = {hello=2, abc=1}

Internally, these conversion work by setting up a "wrapper" object that forwards all operations to the underlying collection object. So collections are never copied when converting between Java and Scala. An interesting property is that if you do a round-trip conversion from, say a Java type to its corresponding Scala type, and back to the same Java type, you end up with the identical collection object you have started with.

The are some other common Scala collections than can also be converted to Java types, but which to not have a corresponding conversion in the other sense. These are:

    Seq           =>    java.util.List 
    mutable.Seq   =>    java.utl.List
    Set           =>    java.util.Set 
    Map           =>    java.util.Map 

Because Java does not distinguish between mutable and immutable collections in their type, a conversion from, say, `scala.immutable.List` will yield a `java.util.List`, where all mutation operations throw an "UnsupportedOperationException". Here's an example:

    scala> jul = List(1, 2, 3)
    jul: java.util.List[Int] = [1, 2, 3]
    scala> jul.add(7)
    java.lang.UnsupportedOperationException
            at java.util.AbstractList.add(AbstractList.java:131)

