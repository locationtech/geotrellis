---
layout: overview-large
title: Architecture of the Parallel Collections Library

disqus: true

partof: parallel-collections
num: 5
---

Like the normal, sequential collections library, Scala's parallel collections
library contains a large number of collection operations which exist uniformly
on many different parallel collection implementations. And like the sequential
collections library, Scala's parallel collections library seeks to prevent
code duplication by likewise implementing most operations in terms of parallel
collection "templates" which need only be defined once and can be flexibly
inherited by many different parallel collection implementations.

The benefits of this approach are greatly eased **maintenance** and
**extensibility**. In the case of maintenance-- by having a single
implementation of a parallel collections operation inherited by all parallel
collections, maintenance becomes easier and more robust; bug fixes propagate
down the class hierarchy, rather than needing implementations to be
duplicated. For the same reasons, the entire library becomes easier to
extend-- new collection classes can simply inherit most of their operations.

## Core Abstractions

The aforementioned "template" traits implement most parallel operations in
terms of two core abstractions-- `Splitter`s and `Combiner`s.

### Splitters

The job of a `Splitter`, as its name suggests, is to split a parallel
collection into a non-trival partition of its elements. The basic idea is to
split the collection into smaller parts until they are small enough to be
operated on sequentially.

    trait Splitter[T] extends Iterator[T] {
    	def split: Seq[Splitter[T]]
    }

Interestingly, `Splitter`s are implemented as `Iterator`s, meaning that apart
from splitting, they are also used by the framework to traverse a  parallel
collection (that is, they inherit standard  methods on `Iterator`s such as
`next` and `hasNext`.) What's unique about this "splitting iterator" is that,
its `split` method splits `this` (again, a `Splitter`, a type of `Iterator`)
further into additional `Splitter`s which each traverse over **disjoint**
subsets of elements of the whole parallel collection. And similar to normal
`Iterator`s, a `Splitter` is invalidated after its `split` method is invoked.

In general, collections are partitioned using `Splitter`s into subsets of
roughly the same size. In cases where more arbitrarily-sized partions are
required, in particular on parallel sequences, a `PreciseSplitter` is used,
which inherits `Splitter` and additionally implements a precise split method,
`psplit`.

### Combiners

`Combiner`s can be thought of as a generalized `Builder`, from Scala's sequential
collections library. Each parallel collection provides a separate `Combiner`,
in the same way that each sequential collection provides a `Builder`.

While in the case of sequential collections, elements can be added to a
`Builder`, and a collection can be produced by invoking the `result` method,
in the case of parallel collections, a `Combiner` has a method called
`combine` which takes another `Combiner` and produces a new `Combiner` that
contains the union of both's elements. After `combine` has been invoked, both
`Combiner`s become invalidated.

    trait Combiner[Elem, To] extends Builder[Elem, To] {
    	def combine(other: Combiner[Elem, To]): Combiner[Elem, To]
    }

The two type parameters `Elem` and `To` above simply denote the element type
and the type of the resulting collection, respectively.

_Note:_ Given two `Combiner`s, `c1` and `c2` where `c1 eq c2` is `true`
(meaning they're the same `Combiner`), invoking `c1.combine(c2)` always does
nothing and simpy returns the receiving `Combiner`, `c1`.

## Hierarchy

Scala's parallel collection's draws much inspiration from the design of
Scala's (sequential) collections library-- as a matter of fact, it mirrors the
regular collections framework's corresponding traits, as shown below.

[<img src="{{ site.baseurl }}/resources/images/parallel-collections-hierarchy.png" width="550">]({{ site.baseurl }}/resources/images/parallel-collections-hierarchy.png)

<center><b>Hierarchy of Scala's Collections and Parallel Collections Libraries</b></center>
<br/>

The goal is of course to integrate parallel collections as tightly as possible
with sequential collections, so as to allow for straightforward substitution
of sequential and parallel collections.

In order to be able to have a reference to a collection which may be either
sequential or parallel (such that it's possible to "toggle" between a parallel
collection and a sequential collection by invoking `par` and `seq`,
respectively), there has to exist a common supertype of both collection types.
This is the origin of the "general" traits shown above, `GenTraversable`,
`GenIterable`, `GenSeq`, `GenMap` and `GenSet`, which don't guarantee in-order
or one-at-a-time traversal. Corresponding sequential or parallel traits
inherit from these. For example, a `ParSeq` and `Seq` are both subtypes of a
general sequence `GenSeq`, but they are in no inheritance relationship with
respect to each other.

For a more detailed discussion of hierarchy shared between sequential and
parallel collections, see the technical report. \[[1][1]\]

## References

1. [On a Generic Parallel Collection Framework, Aleksandar Prokopec, Phil Bawgell, Tiark Rompf, Martin Odersky, June 2011][1]

[1]: http://infoscience.epfl.ch/record/165523/files/techrep.pdf "flawed-benchmark"
