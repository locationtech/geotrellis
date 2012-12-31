---
layout: overview-large
title: Introduction

disqus: true

partof: collections
num: 1
languages: [ja]
---

**Martin Odersky, and Lex Spoon**
  
In the eyes of many, the new collections framework is the most significant
change in the Scala 2.8 release. Scala had collections before (and in fact the new
framework is largely compatible with them). But it's only 2.8 that
provides a common, uniform, and all-encompassing framework for
collection types.

Even though the additions to collections are subtle at first glance,
the changes they can provoke in your programming style can be
profound.  In fact, quite often it's as if you work on a higher-level
with the basic building blocks of a program being whole collections
instead of their elements. This new style of programming requires some
adaptation. Fortunately, the adaptation is helped by several nice
properties of the new Scala collections. They are easy to use,
concise, safe, fast, universal.

**Easy to use:** A small vocabulary of 20-50 methods is
enough to solve most collection problems in a couple of operations. No
need to wrap your head around complicated looping structures or
recursions. Persistent collections and side-effect-free operations mean
that you need not worry about accidentally corrupting existing
collections with new data.  Interference between iterators and
collection updates is eliminated.

**Concise:** You can achieve with a single word what used to
take one or several loops. You can express functional operations with
lightweight syntax and combine operations effortlessly, so that the result
feels like a custom algebra.  

**Safe:** This one has to be experienced to sink in. The
statically typed and functional nature of Scala's collections means
that the overwhelming majority of errors you might make are caught at
compile-time. The reason is that (1) the collection operations
themselves are heavily used and therefore well
tested. (2) the usages of the collection operation make inputs and
output explicit as function parameters and results. (3) These explicit
inputs and outputs are subject to static type checking. The bottom line
is that the large majority of misuses will manifest themselves as type
errors. It's not at all uncommon to have programs of several hundred
lines run at first try.

**Fast:** Collection operations are tuned and optimized in the
libraries. As a result, using collections is typically quite
efficient. You might be able to do a little bit better with carefully
hand-tuned data structures and operations, but you might also do a lot
worse by making some suboptimal implementation decisions along the
way.  What's more, collections have been recently adapted to parallel
execution on multi-cores. Parallel collections support the same
operations as sequential ones, so no new operations need to be learned
and no code needs to be rewritten. You can turn a sequential collection into a
parallel one simply by invoking the `par` method.

**Universal:** Collections provide the same operations on
any type where it makes sense to do so. So you can achieve a lot with
a fairly small vocabulary of operations. For instance, a string is
conceptually a sequence of characters. Consequently, in Scala
collections, strings support all sequence operations. The same holds
for arrays.

**Example:** Here's one line of code that demonstrates many of the 
advantages of Scala's collections.

    val (minors, adults) = people partition (_.age < 18)

It's immediately clear what this operation does: It partitions a
collection of `people` into `minors` and `adults` depending on
their age. Because the `partition` method is defined in the root
collection type `TraversableLike`, this code works for any kind of
collection, including arrays. The resulting `minors` and `adults`
collections will be of the same type as the `people` collection.

This code is much more concise than the one to three loops required for
traditional collection processing (three loops for an array, because
the intermediate results need to be buffered somewhere else).  Once
you have learned the basic collection vocabulary you will also find
writing this code is much easier and safer than writing explicit
loops. Furthermore, the `partition` operation is quite fast, and will
get even faster on parallel collections on multi-cores.  (Parallel
collections have been released
as part of Scala 2.9.)

This document provides an in depth discussion of the APIs of the
Scala collections classes from a user perspective.  They take you on
a tour of all the fundamental classes and the methods they define.