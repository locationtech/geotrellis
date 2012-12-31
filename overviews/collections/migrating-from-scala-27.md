---
layout: overview-large
title: Migrating from Scala 2.7

disqus: true

partof: collections
num: 18
outof: 18
languages: [ja]
---

Porting your existing Scala applications to use the new collections should be almost automatic. There are only a couple of possible issues to take care of.

Generally, the old functionality of Scala 2.7 collections has been left in place. Some features have been deprecated, which means they will removed in some future release. You will get a _deprecation warning_ when you compile code that makes use of these features in Scala 2.8. In a few places deprecation was unfeasible, because the operation in question was retained in 2.8, but changed in meaning or performance characteristics. These cases will be flagged with _migration warnings_ when compiled under 2.8. To get full deprecation and migration warnings with suggestions how to change your code, pass the `-deprecation` and `-Xmigration` flags to `scalac` (note that `-Xmigration` is an extended option, so it starts with an `X`.) You can also pass the same options to the `scala` REPL to get the warnings in an interactive session. Example:

    >scala -deprecation -Xmigration
    Welcome to Scala version 2.8.0.final
    Type in expressions to have them evaluated.
    Type :help for more information.
    scala> val xs = List((1, 2), (3, 4))
    xs: List[(Int, Int)] = List((1,2), (3,4))
    scala> List.unzip(xs)
    <console>:7: warning: method unzip in object List is deprecated: use xs.unzip instead of List.unzip(xs)
           List.unzip(xs)
                ^
    res0: (List[Int], List[Int]) = (List(1, 3),List(2, 4))
    scala> xs.unzip
    res1: (List[Int], List[Int]) = (List(1, 3),List(2, 4))
    scala> val m = xs.toMap
    m: scala.collection.immutable.Map[Int,Int] = Map((1,2), (3,4))
    scala> m.keys
    <console>:8: warning: method keys in trait MapLike has changed semantics:
    As of 2.8, keys returns Iterable[A] rather than Iterator[A].
           m.keys
             ^
    res2: Iterable[Int] = Set(1, 3)

There are two parts of the old libraries which have been replaced wholesale, and for which deprecation warnings were not feasible.

1. The previous `scala.collection.jcl` package is gone. This package tried to mimick some of the Java collection library design in Scala, but in doing so broke many symmetries. Most people who wanted Java collections bypassed `jcl` and used `java.util` directly. Scala 2.8 offers automatic conversion mechanisms between both collection libraries in the [JavaConversions]({{ site.baseurl }}/overviews/collections/conversions-between-java-and-scala-collections.md) object which replaces the `jcl` package.
2. Projections have been generalized and cleaned up and are now available as views. It seems that projections were used rarely, so not much code should be affected by this change.

So, if your code uses either `jcl` or projections there might be some minor rewriting to do.

