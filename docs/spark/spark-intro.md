# geotrellis.spark

## Distributed Computation

**NOTE: Distributed computing is difficult to get right. Luckily, we
are able to lean on the `RDD` abstraction provided by Apache Spark to
simplify matters somewhat. Still, the conceptual difficulties in
`geotrellis.spark` are arguably as great as can be found in any part
of the GeoTrellis library. As such, the discussion in this portion of
the documentation assumes a passing familiarity with the key concepts
of `geotrellis.raster`. If this is a difficulty, please refer to the
[documentation for `geotrellis.raster`](../raster/raster-intro.md).**  

Consider the (relatively) simple case of carrying out local addition on two
raster tiles. In the abstract, this merely involves adding together
corresponding values from two different `Tile`s. Practically, things can
quickly become more complex: what if one `Tile`'s data covers a larger
extent than the other? In general, how do we determine what
'corresponding values' means in such a context? (Some specifics related
to this question are covered in the `geotrellis.spark` [documentation on
joins](./spark-joins.md))  

What we need, then, is to pass around tiles as well as some kind of
associated data. In addition, the `Tile` abstraction makes sense only
in a particular place (in space and/or time) - the data in my `Tile`
represents the elevation of terrain in this or that actual place which
has such and such spatial relations to other `Tile`s that represent
neighboring terrain elevations. If your application for finding directions
displayed street data for Beijing in the middle of downtown
Philadelphia, it would be extremely difficult to actually use. From the
perspective of application performance during spatially-aware
computations (say, for instance, that I want to compute the average
elevation for every `Tile`'s cell within a five mile radius of a target
location) it is also useful to have an index which provides a sort of
shortcut for tracking down the relevant information.  

The need for intelligently indexed tiles is especially great when
thinking about distributing work over those tiles across multiple
computers. The tightest bottleneck in such a configuration is the
communication between different nodes in the network. What follows is
that reducing the likelihood of communication between nodes is one
of the best ways to improve performance. Having intelligently indexed
tilesets allows us to partition data according to expectations about
which `Tile`s each node will require to calculate its results.  

Hopefully you're convinced that for a wide variety of GeoTrellis
use-cases it makes sense to pass around tiles with indices to which
they correspond as well as metadata. This set of concerns is encoded in
the type system as `RDD[(K, V)] with Metadata[M]`.


## Core types (and recurring type parameters) in geotrellis.spark

> Spark revolves around the concept of a resilient distributed dataset
> (RDD), which is a fault-tolerant collection of elements that can be
> operated on in parallel.
— [Apache Spark
Documentation](http://spark.apache.org/docs/1.6.0/programming-guide.html#resilient-distributed-datasets-rdds)
(version 1.6.0)


### RDD[(K, V)] with Metadata[M]

The conceptual core of GeoTrellis' Spark support is the `RDD[(K, V)]
with Metadata[M]`. Let's look at the type parameters and get a sense of
their meaning (these will correspond to the set of concerns discussed in
the introductory remarks above):
`RDDs[(K, V)]`s are distributed collections of `(K, V)`s. `K` corresponds
to the index (or key, thus `K`) for a given tile, `V` (or value, thus `V`).
`Metadata[M]`s are things which have some associated metadata: as simple
as that. The only guarantee this `trait` provides is that there exists a
publicly accessible `val` on it, 'metadata', which is an instance of some
type, `M`.  

So far we've spoken in the (extreme) abstract about `K`s, `V`s, and
`M`s. This design makes it entirely possible to create your own concrete
types which lean on GeoTrellis code for storing and doing distributed
work. GeoTrellis also ships with some concrete types which are designed
specifically to distribute the kinds of operations you can find in
`geotrellis.raster`.  

- [More on `K`](./spark-keys.md)
- [More on `V`](./spark-values.md)
- [More on `M`](./spark-metadata.md)



## ContextRDD[K, V, M] is not the type you're looking for

**NOTE: Don't use this as a type signature - this is just sugar for
creating an rdd with metadata**

The `ContextRDD[K, V, M]` is a subclass of `RDD[(K, V)] with
Metadata[M]`. It provides the ability to construct `RDD[(K, V)] with
Metadata[M]` instances given an `RDD[(K, V)]` and some metadata `M`.

