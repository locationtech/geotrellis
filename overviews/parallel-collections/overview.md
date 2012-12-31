---
layout: overview-large
title: Overview

disqus: true

partof: parallel-collections
num: 1
---

**Aleksandar Prokopec, Heather Miller**

## Motivation

Amidst the shift in recent years by processor manufacturers from single to
multi-core architectures, academia and industry alike have conceded that
_Popular Parallel Programming_ remains a formidable challenge.

Parallel collections were included in the Scala standard library in an effort
to facilitate parallel programming by sparing users from low-level
parallelization details, meanwhile providing them with a familiar and simple
high-level abstraction. The hope was, and still is, that implicit parallelism
behind a collections abstraction will bring reliable parallel execution one
step closer to the workflow of mainstream developers.

The idea is simple-- collections are a well-understood and frequently-used
programming abstraction. And given their regularity, they're able to be
efficiently parallelized, transparently. By allowing a user to "swap out"
sequential collections for ones that are operated on in parallel, Scala's
parallel collections take a large step forward in enabling parallelism to be
easily brought into more code.

Take the following, sequential example, where we perform a monadic operation
on some large collection:

    val list = (1 to 10000).toList
    list.map(_ + 42)
    
To perform the same operation in parallel, one must simply invoke the `par`
method on the sequential collection, `list`. After that, one can use a
parallel collection in the same way one would normally use a sequential
collection. The above example can be parallelized by simply doing the
following:

    list.par.map(_ + 42)

The design of Scala's parallel collections library is inspired by and deeply
integrated with Scala's (sequential) collections library (introduced in 2.8).
It provides a parallel counterpart to a number of important data structures
from Scala's (sequential) collection library, including:

* `ParArray`
* `ParVector`
* `mutable.ParHashMap`
* `mutable.ParHashSet`
* `immutable.ParHashMap`
* `immutable.ParHashSet`
* `ParRange`
* `ParTrieMap` (`collection.concurrent.TrieMap`s are new in 2.10)

In addition to a common architecture, Scala's parallel collections library
additionally shares _extensibility_ with the sequential collections library.
That is, like normal sequential collections, users can integrate their own
collection types and automatically inherit all of the predefined (parallel)
operations available on the other parallel collections in the standard
library.

## Some Examples

To attempt to illustrate the generality and utility of parallel collections,
we provide a handful of simple example usages, all of which are transparently
executed in parallel.

_Note:_ Some of the following examples operate on small collections, which
isn't recommended. They're provided as examples for illustrative purposes
only. As a general heuristic, speed-ups tend to be noticeable when the size of
the collection is large, typically several thousand elements. (For more
information on the relationship between the size of a parallel collection and
performance, please see the 
[appropriate subsection]({{ site.baseurl}}/overviews/parallel-collections/performance.html#how_big_should_a_collection_be_to_go_parallel) of the [performance]({{ site.baseurl }}/overviews/parallel-collections/performance.html) 
section of this guide.)

#### map

Using a parallel `map` to transform a collection of `String` to all-uppercase:

    scala> val lastNames = List("Smith","Jones","Frankenstein","Bach","Jackson","Rodin").par
    lastNames: scala.collection.parallel.immutable.ParSeq[String] = ParVector(Smith, Jones, Frankenstein, Bach, Jackson, Rodin)
    
    scala> lastNames.map(_.toUpperCase)
    res0: scala.collection.parallel.immutable.ParSeq[String] = ParVector(SMITH, JONES, FRANKENSTEIN, BACH, JACKSON, RODIN)

#### fold

Summing via `fold` on a `ParArray`:

    scala> val parArray = (1 to 1000000).toArray.par    
    parArray: scala.collection.parallel.mutable.ParArray[Int] = ParArray(1, 2, 3, ...
    
    scala> parArray.fold(0)(_ + _)
    res0: Int = 1784293664

#### filter

Using a parallel `filter` to select the last names that come alphabetically
after the letter "K".

    scala> val lastNames = List("Smith","Jones","Frankenstein","Bach","Jackson","Rodin").par
    lastNames: scala.collection.parallel.immutable.ParSeq[String] = ParVector(Smith, Jones, Frankenstein, Bach, Jackson, Rodin)
    
    scala> lastNames.filter(_.head >= 'J')
    res0: scala.collection.parallel.immutable.ParSeq[String] = ParVector(Smith, Jones, Jackson, Rodin)

## Creating a Parallel Collection

Parallel collections are meant to be used in exactly the same way as
sequential collections-- the only noteworthy difference is how to _obtain_ a
parallel collection.

Generally, one has two choices for creating a parallel collection:

First, by using the `new` keyword and a proper import statement: 

    import scala.collection.parallel.immutable.ParVector
    val pv = new ParVector[Int]

Second, by _converting_ from a sequential collection:

    val pv = Vector(1,2,3,4,5,6,7,8,9).par

What's important to expand upon here are these conversion methods-- sequential
collections can be converted to parallel collections by invoking the
sequential collection's `par` method, and likewise, parallel collections can
be converted to sequential collections by invoking the parallel collection's
`seq` method.

_Of Note:_ Collections that are inherently sequential (in the sense that the
elements must be accessed one after the other), like lists, queues, and
streams, are converted to their parallel counterparts by copying the elements
into a similar parallel collection. An example is `List`-- it's converted into
a standard immutable parallel sequence, which is a `ParVector`. Of course, the
copying required for these collection types introduces an overhead not
incurred by any other collection types, like `Array`, `Vector`, `HashMap`, etc.

For more information on conversions on parallel collections, see the
[conversions]({{ site.baseurl }}/overviews/parallel-collections/conversions.html) 
and [concrete parallel collection classes]({{ site.baseurl }}/overviews/parallel-collections/concrete-parallel-collections.html) 
sections of this guide.

## Semantics

While the parallel collections abstraction feels very much the same as normal
sequential collections, it's important to note that its semantics differs,
especially with regards to side-effects and non-associative operations.

In order to see how this is the case, first, we visualize _how_ operations are
performed in parallel. Conceptually, Scala's parallel collections framework
parallelizes an operation on a parallel collection by recursively "splitting"
a given collection, applying an operation on each partition of the collection
in parallel, and re-"combining" all of the results that were completed in
parallel. 

These concurrent, and "out-of-order" semantics of parallel collections lead to
the following two implications:

1. **Side-effecting operations can lead to non-determinism**
2. **Non-associative operations lead to non-determinism**

### Side-Effecting Operations

Given the _concurrent_ execution semantics of the parallel collections
framework, operations performed on a collection which cause side-effects
should generally be avoided, in order to maintain determinism. A simple
example is by using an accessor method, like `foreach` to increment a `var`
declared outside of the closure which is passed to `foreach`.

    scala> var sum = 0
    sum: Int = 0

    scala> val list = (1 to 1000).toList.par
    list: scala.collection.parallel.immutable.ParSeq[Int] = ParVector(1, 2, 3,…
    
    scala> list.foreach(sum += _); sum
    res01: Int = 467766
    
    scala> var sum = 0
    sum: Int = 0
    
    scala> list.foreach(sum += _); sum
    res02: Int = 457073    
    
    scala> var sum = 0
    sum: Int = 0
    
    scala> list.foreach(sum += _); sum
    res03: Int = 468520    
    
Here, we can see that each time `sum` is reinitialized to 0, and `foreach` is
called again on `list`, `sum` holds a different value. The source of this 
non-determinism is a _data race_-- concurrent reads/writes to the same mutable
variable.

In the above example, it's possible for two threads to read the _same_ value
in `sum`, to spend some time doing some operation on that value of `sum`, and
then to attempt to write a new value to `sum`, potentially resulting in an
overwrite (and thus, loss) of a valuable result, as illustrated below:

    ThreadA: read value in sum, sum = 0                value in sum: 0
    ThreadB: read value in sum, sum = 0                value in sum: 0
    ThreadA: increment sum by 760, write sum = 760     value in sum: 760
    ThreadB: increment sum by 12, write sum = 12       value in sum: 12

The above example illustrates a scenario where two threads read the same
value, `0`, before one or the other can sum `0` with an element from their
partition of the parallel collection. In this case, `ThreadA` reads `0` and
sums it with its element, `0+760`, and in the case of `ThreadB`, sums `0` with
its element, `0+12`. After computing their respective sums, they each write
their computed value in `sum`. Since `ThreadA` beats `ThreadB`, it writes
first, only for the value in `sum` to be overwritten shortly after by
`ThreadB`, in effect completely overwriting (and thus losing) the value `760`.

### Non-Associative Operations

Given this _"out-of-order"_ semantics,  also must be careful to perform only
associative operations in order to avoid non-determinism. That is, given a
parallel collection, `pcoll`, one should be sure that when invoking a 
higher-order function on `pcoll`, such as `pcoll.reduce(func)`, the order in 
which `func` is applied to the elements of `pcoll` can be arbitrary. A simple, 
but obvious example is a non-associative operation such as subtraction:

    scala> val list = (1 to 1000).toList.par
    list: scala.collection.parallel.immutable.ParSeq[Int] = ParVector(1, 2, 3,…
    
    scala> list.reduce(_-_)
    res01: Int = -228888
    
    scala> list.reduce(_-_)
    res02: Int = -61000
    
    scala> list.reduce(_-_)
    res03: Int = -331818
    
In the above example, we take a `ParVector[Int]`, invoke `reduce`, and pass to
it `_-_`, which simply takes two unnamed elements, and subtracts the first
from the second. Due to the fact that the parallel collections framework spawns
threads which, in effect, independently perform `reduce(_-_)` on different
sections of the collection, the result of two runs of `reduce(_-_)` on the
same collection will not be the same.

_Note:_ Often, it is thought that, like non-associative operations,  non-commutative 
operations passed to a higher-order function on a parallel
collection likewise result in non-deterministic behavior. This is not the
case, a simple example is string concatenation-- an associative, but non-
commutative operation:

    scala> val strings = List("abc","def","ghi","jk","lmnop","qrs","tuv","wx","yz").par
    strings: scala.collection.parallel.immutable.ParSeq[java.lang.String] = ParVector(abc, def, ghi, jk, lmnop, qrs, tuv, wx, yz) 
    
    scala> val alphabet = strings.reduce(_++_)
    alphabet: java.lang.String = abcdefghijklmnopqrstuvwxyz

The _"out of order"_ semantics of parallel collections only means that
the operation will be executed out of order (in a _temporal_ sense. That is,
non-sequentially), it does not mean that the result will be
re-"*combined*" out of order (in a _spatial_ sense). On the contrary, results
will generally always be reassembled _in order_-- that is, a parallel collection
broken into partitions A, B, C, in that order, will be reassembled once again
in the order A, B, C. Not some other arbitrary order like B, C, A.

For more on how parallel collections split and combine operations on different
parallel collection types, see the [Architecture]({{ site.baseurl }}/overviews
/parallel-collections/architecture.html) section of this guide. 

