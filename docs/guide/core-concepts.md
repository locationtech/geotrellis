Geographical Information Systems (GIS), like any specialized field, has a
wealth of jargon and unique concepts. When represented in software, these
concepts can sometimes be skewed or expanded from their original forms. We
give a thorough definition of many of the core concepts here, while
referencing the Geotrellis objects and source files backing them.

This document aims to be informative to new and experienced GIS users alike.
If GIS is brand, brand new to you, [this document](https://www.gislounge.com/what-is-gis/)
is a useful high level overview.

Raster Data
===========

> “Yes raster is faster, but raster is vaster and vector just SEEMS more corrector.”
— [C. Dana Tomlin](http://uregina.ca/piwowarj/NotableQuotables.html)

**Raster vs Tile**

The entire purpose of `geotrellis.raster` is to provide primitive datatypes
which implement, modify, and utilize rasters. In GeoTrellis, a raster is
just a tile with an associated extent (read about extents below). A tile is
just a two-dimensional, collection of evenly spaced data. Tiles are a lot
like certain sequences of sequences (this array of arrays is like a 3x3
tile):

```scala
val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
/** It probably looks more like your mental model if we stack them up:
  * [[1,1,1],
  *  [1,2,2],
  *  [1,2,3]]
  */
```

In the raster module of GeoTrellis, the base type of tile is just `Tile`.
All GeoTrellis compatible tiles will have inherited from that base class, so
if you find yourself wondering what a given type of tile's powers are,
that's a decent place to start your search. Here's an incomplete list of the
types of things on offer (Seriously, check out
[the source code](../../raster/src/main/scala/geotrellis/raster/Tile.scala)!
It *will* clarify the semantics of tiles in GeoTrellis.):

- Mapping transformations of arbitrary complexity over the constituent cells
- Carrying out operations (side-effects) for each cell
- Querying a specific tile value
- Rescaling, resampling, cropping

As we've already discussed, tiles are made up of squares which contain
values. We'll sometimes refer to these value-boxes as 'cells'. And, just
like cells in the body, though they are discrete units, they're most
interesting when looked at from a more holistic perspective - rasters encode
relations between values in a uniform space and it is usually these
relations which most interest us. The code found in the `mapalgebra`
submodule — discussed later in this document — is all about exploiting these
spatial relations.

**Working with cell values**

One of the first questions you'll ask yourself when working with GeoTrellis
is what kinds of representation best model the domain you're dealing with.
What types of value do you need your raster to hold? This question is the
province of GeoTrellis `CellType`s. See below for a description of `Cell
Types`

**Building Your Own Tiles**

With a grasp of tiles and `CellType`s, we've got all the conceptual tools
necessary to construct our own tiles. Now, since a tile is a combination of
a `CellType` with which its cells are encoded and their spatial arrangement,
we will have to somehow combine `Tile` (which encodes our expectations about
how cells sit with respect to one another) and the datatype of our choosing.
Luckily, GeoTrellis has done this for us. To keep its users sane, the wise
maintainers of GeoTrellis have organized `geotrellis.raster` such that fully
reified tiles sit at the bottom of an pretty simple inheritance chain. Let's
explore that inheritance so that you will know where to look when your
intuitions lead you astray:

From IntArrayTile.scala:
```scala
final case class IntArrayTile(array: Array[Int], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile
```

From DoubleArrayTile.scala:
```scala
final case class DoubleArrayTile(array: Array[Double], cols: Int, rows: Int)
  extends MutableArrayTile with DoubleBasedArrayTile
```

**Tile inheritance structure**

It looks like there are two different chains of inheritance here
(`IntBasedArrayTile` and `DoubleBasedArrayTile`). Let's first look at what
they share:

- `MutableArrayTile` adds some nifty methods for in-place manipulation of
cells (GeoTrellis is about performance, so this minor affront to the gods of
immutability can be forgiven). From MutableArrayTile.scala:

```scala
trait MutableArrayTile extends ArrayTile
```

- One level up is `ArrayTile`. It's handy because it implements the
behavior which largely allows us to treat our tiles like big, long arrays of
(arrays of) data. They also have the trait `Serializable`, which is neat any
time you can't completely conduct your business within the neatly defined
space-time of the JVM processes which are running on a single machine (this
is the point of GeoTrellis' Spark integration). From ArrayTile.scala:

```scala
trait ArrayTile extends Tile with Serializable
```

- At the top rung in our abstraction ladder we have `Tile`. You might be
surprised how much we can say about tile behavior from the base of its
inheritance tree, so (at risk of sounding redundant) the source is worth
spending some time on. From Tile.scala:

```scala
trait Tile
```

Cool. That wraps up one half of the inheritance. But how about that the
features they don't share? As it turns out, each reified tile's second piece
of inheritance merely implements methods for dealing with their constitutent
`CellType`s. From IntBasedArrayTile.scala:

```scala
trait IntBasedArrayTile {
  def apply(i:Int):Int
  def update(i:Int, z:Int):Unit

  def applyDouble(i:Int):Double = i2d(apply(i))
  def updateDouble(i:Int, z:Double):Unit = update(i, d2i(z))
}
```

From DoubleBasedArrayTile.scala:

```scala
trait DoubleBasedArray {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}
```

Mostly we've been looking to tiny snippets of source, but the two above are
the entire files. All they do is:

1. Tell the things that inherit from them that they'd better define methods
for application and updating of values that look like their cells if they
want the compiler to be happy.
2. Tell the things that inherit from them exactly how to take values which
don't look like their cells (int-like things for `DoubleBasedArray` and
double-like things for `IntBasedArray`) and turn them into types they find
more palatable.

As it turns out, `CellType` is one of those things that we can *mostly* ignore
once we've settled on which one is proper for our domain. After all, it appears
as though there's very little difference between tiles that prefer int-like
things and tiles that prefer double-like things.

> **CAUTION**: While it is true, in general, that operations are `CellType`
agnostic, both `get` and `getDouble` are methods implemented on `Tile`. In
effect, this means that you'll want to be careful when querying values. If
you're working with int-like `CellType`s, probably use `get`. If you're
working with float-like `CellType`s, usually you'll want `getDouble`.

**Taking our tiles out for a spin**

In the repl, you can try this out:

```scala
import geotrellis.raster._
import geotrellis.vector._

scala> IntArrayTile(Array(1,2,3),1,3)
res0: geotrellis.raster.IntArrayTile = IntArrayTile([S@338514ad,1,3)

scala> IntArrayTile(Array(1,2,3),3,1)
res1: geotrellis.raster.IntArrayTile = IntArrayTile([S@736a81de,3,1)

scala> IntArrayTile(Array(1,2,3,4,5,6,7,8,9),3,3)
res2: geotrellis.raster.IntArrayTile = IntArrayTile([I@5466441b,3,3)
```

**Constructing a Raster**

```scala
scala> Extent(0, 0, 1, 1)
res4: geotrellis.vector.Extent = Extent(0.0,0.0,1.0,1.0)

scala> Raster(res2, res4)
res5: geotrellis.raster.Raster = Raster(IntArrayTile([I@7b47ab7,1,3),Extent(0.0,0.0,1.0,1.0))
```

Here's a fun method for exploring your tiles:

```scala
scala> res0.asciiDraw()
res3: String =
"    1
     2
     3
"

scala> res2.asciiDraw()
res4: String =
"    1     2     3
     4     5     6
     7     8     9
"
```

That's probably enough to get started. `geotrellis.raster` is a pretty big
place, so you'll benefit from spending a few hours playing with the tools it
provides.

Vector Data
===========

Vector Tiles
============

Invented by [Mapbox](https://www.mapbox.com/), VectorTiles are a combination
of the ideas of finite-sized tiles and vector geometries. Mapbox maintains
the official implementation spec for VectorTile codecs. The specification is
free and open source.

VectorTiles are advantageous over raster tiles in that:

  - They are typically smaller to store
  - They can be easily transformed (rotated, etc.) in real time
  - They allow for continuous (as opposed to step-wise) zoom in Slippy Maps.

Raw VectorTile data is stored in the protobuf format. Any codec implementing
[the spec](https://github.com/mapbox/vector-tile-spec/tree/master/2.1) must
decode and encode data according to [this `.proto`
schema](https://github.com/mapbox/vector-tile-spec/blob/master/2.1/vector_tile.proto).

GeoTrellis provides the `geotrellis-vectortile` module, a high-performance
implementation of **Version 2.1** of the VectorTile spec. It features:

- Decoding of **Version 2** VectorTiles from Protobuf byte data into useful Geotrellis types.
- Lazy decoding of Geometries. Only parse what you need!
- Read/write VectorTile layers to/from any of our backends.

Ingests of raw vector data into VectorTile sets is still pending (***as of 2016 October 28***)

**Small Example**

```
import geotrellis.spark.SpatialKey
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile
import geotrellis.vectortile.protobuf._

val bytes: Array[Byte] = ...  // from some `.mvt` file
val key: SpatialKey = ...  // preknown
val layout: LayoutDefinition = ...  // preknown
val tileExtent: Extent = layout.mapTransform(key)

/* Decode Protobuf bytes. */
val tile: VectorTile = ProtobufTile.fromBytes(bytes, tileExtent)

/* Encode a VectorTile back into bytes. */
val encodedBytes: Array[Byte] = tile match {
  case t: ProtobufTile => t.toBytes
  case _ => ???  // Handle other backends or throw errors.
}
```

See [our VectorTile
Scaladocs](https://geotrellis.github.io/scaladocs/latest/#geotrellis.vectortile.package)
for detailed usage information.

**Implementation Assumptions**

This particular implementation of the VectorTile spec makes the following
assumptions:

- Geometries are implicitly encoded in ''some'' Coordinate Reference
  system. That is, there is no such thing as a "projectionless" VectorTile.
  When decoding a VectorTile, we must provide a Geotrellis [[Extent]] that
  represents the Tile's area on a map.
  With this, the grid coordinates stored in the VectorTile's Geometry are
  shifted from their
  original [0,4096] range to actual world coordinates in the Extent's CRS.
- The `id` field in VectorTile Features doesn't matter.
- `UNKNOWN` geometries are safe to ignore.
- If a VectorTile `geometry` list marked as `POINT` has only one pair
  of coordinates, it will be decoded as a Geotrellis `Point`. If it has
  more than one pair, it will be decoded as a `MultiPoint`. Likewise for
  the `LINESTRING` and `POLYGON` types. A complaint has been made about
  the spec regarding this, and future versions may include a difference
  between single and multi geometries.


Tile Layers
===========

Tile layers (Rasters or otherwise) are represented in GeoTrellis with the
type `RDD[(K, V)] with Metadata[M]`. This type is used extensively across
the code base, and its contents form the deepest compositional hierarchy we
have:

![](images/type-composition.png)

In this diagram:

- `CustomTile`, `CustomMetadata`, and `CustomKey` don't exist, they
represent types that you could write yourself for your application.
- The `K` seen in several places is the same `K`.
- The type `RDD[(K, V)] with Metadata[M]` is a Scala *Anonymous Type*. In
this case, it means `RDD` from Apache Spark with extra methods injected from
the `Metadata` trait. This type is sometimes aliased in GeoTrellis as
`ContextRDD`.
- `RDD[(K, V)]` resembles a Scala `Map[K, V]`, and in fact has further
`Map`-like methods injected by Spark when it takes this shape. See Spark's
[PairRDDFunctions](http://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.rdd.PairRDDFunctions)
Scaladocs for those methods. **Note:** Unlike `Map`, the `K`s here are
**not** guaranteed to be unique.

**TileLayerRDD**

A common specification of `RDD[(K, V)] with Metadata[M]` in GeoTrellis is as follows:

```scala
type TileLayerRDD[K] = RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]
```

This type represents a grid (or cube!) of `Tile`s on the earth, arranged
according to some `K`. Features of this grid are:

- Grid location `(0, 0)` is the top-leftmost `Tile`.
- The `Tile`s exist in *some* CRS. In `TileLayerMetadata`, this is kept
track of with an actual `CRS` field.
- In applications, `K` is mostly `SpatialKey` or `SpaceTimeKey`.

**Tile Layer IO**

Layer IO requires a [Tile Layer Backend](./tile-backends.md). Each backend
has an `AttributeStore`, a `LayerReader`, and a `LayerWriter`.

Example setup (with our `File` system backend):

```scala
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.file._

val catalogPath: String = ...  /* Some location on your computer */

val store: AttributeStore = FileAttributeStore(catalogPath)

val reader = FileLayerReader(store)
val writer = FileLayerWriter(store)
```

Writing an entire layer:

```scala
/* Zoom level 13 */
val layerId = LayerId("myLayer", 13)

/* Produced from an ingest, etc. */
val rdd: TileLayerRDD[SpatialKey] = ...

/* Order your Tiles according to the Z-Curve Space Filling Curve */
val index: KeyIndex[SpatialKey] = ZCurveKeyIndexMethod.createIndex(rdd.metadata.bounds)

/* Returns `Unit` */
writer.write(layerId, rdd, index)
```

Reading an entire layer:

```scala
/* `.read` has many overloads, but this is the simplest */
val sameLayer: TileLayerRDD[SpatialKey] = reader.read(layerId)
```

Querying a layer (a "filtered" read):

```scala
/* Some area on the earth to constrain your query to */
val extent: Extent = ...

/* There are more types that can go into `where` */
val filteredLayer: TileLayerRDD[SpatialKey] =
  reader.query(layerId).where(Intersects(extent)).result
```

Typeclasses
===========

Typeclasses are a common feature of Functional Programming. As stated in
[Cell Types](#cell-types), typeclasses group data types by what they can
*do*, as opposed to by what they *are*. If traditional OO inheritance
arranges classes in a tree hierarchy, typeclasses arrange them in a graph.

Typeclasses are realized in Scala through a combination of `trait`s and
`implicit` class wrappings. A typeclass constraint is visible in a
class/method signature like this:

```scala
class Foo[A: Order](a: A) { ... }
```

Meaning that `Foo` can accept any `A`, so long as it is "orderable". In reality,
this in syntactic sugar for the following:

```scala
class Foo[A](a: A)(implicit ev: Order[A]) { ... }
```

Here's a real-world example from GeoTrellis code:

```scala
protected def _write[
  K: AvroRecordCodec: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat: GetComponent[?, Bounds[K]]
](layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = { ... }
```

A few things to notice:

- Multiple constraints can be given to a single type variable: `K: Foo: Bar: Baz`
- `?` refers to `M`, helping the compiler with type inference. Unfortunately `M: GetComponent[M, Bounds[K]]` is not syntactically possible

Below is a description of the most-used typeclasses used in GeoTrellis. All
are written by us, unless otherwise stated.

**ClassTag**

Built-in from `scala.reflect`. This allows classes to maintain some type
information at runtime, which in GeoTrellis is important for serialization.
You will never need to use this directly, but may have to annotate your
methods with it (the compiler will let you know).

**JsonFormat**

From the `spray` library. This constraint says that its type can be
converted to and from JSON, like this:

```scala
def toJsonAndBack[A: JsonFormat](a: A): A = {
  val json: Value = a.toJson

  json.convertTo[A]
}
```

**AvroRecordCodec**

Any type that can be serialized by [Apache Avro](https://avro.apache.org/).
While references to `AvroRecordCodec` appear frequently through GeoTrellis
code, you will never need to use its methods. They are used internally by
our Tile Layer Backends and Spark.

**Boundable**

Always used on `K`, `Boundable` means your key type has a finite bound.

```scala
trait Boundable[K] extends Serializable {
  def minBound(p1: K, p2: K): K

  def maxBound(p1: K, p2: K): K
...  // etc
}
```

**Component**

`Component` is a bare-bones `Lens`. A `Lens` is a pair of functions that
allow one to generically get and set values in a data structure. They are
particularly useful for nested data structures. `Component` looks like this:

```scala
trait Component[T, C] extends GetComponent[T, C] with SetComponent[T, C]
```

Which reads as "if I have a `T`, I can read a `C` out of it" and "if I have
a `T`, I can write some `C` back into it". The lenses we provide are as follows:

- `SpatialComponent[T]` - read a `SpatialKey` out of a some `T` (usually
`SpatialKey` or `SpaceTimeKey`)
- `TemporalComponent[T]` - read a `TemporalKey` of some `T` (usually
`SpaceTimeKey`)

**Functor**

A *Functor* is anything that maintains its shape and semantics when `map`'d
over. Things like `List`, `Map`, `Option` and even `Future` are Functors.
`Set` and binary trees are not, since `map` could change the size of a `Set`
and the semantics of `BTree`.

Vanilla Scala does not have a `Functor` typeclass, but implements its
functionality anyway. Libraries like [Cats](http://typelevel.org/cats/) and
[ScalaZ](https://github.com/scalaz/scalaz) provide a proper `Functor`, but
their definitions don't allow further constraints on your inner type. We
have:

```scala
trait Functor[F[_], A] extends MethodExtensions[F[A]]{
  /** Lift `f` into `F` and apply to `F[A]`. */
  def map[B](f: A => B): F[B]
}
```

which allows us to do:

```scala
def foo[M[_], K: SpatialComponent: λ[α => M[α] => Functor[M, α]]](mk: M[K]) { ... }
```

which says "`M` can be mapped into, and the `K` you find is guaranteed to
have a `SpatialComponent` as well".

Keys and Key Indexes
====================

Tiles
=====

Cell Types
==========

**What is a Cell Type?**

- A `CellType` is a data type plus a policy for handling cell values that
  may contain no data.
- By 'data type' we shall mean the underlying numerical representation
  of a `Tile`'s cells.
- `NoData`, for performance reasons, is not represented as a value outside
  the range of the underlying data type (as, e.g., `None`) - if each cell in some
  tile is a `Byte`, the `NoData` value of that tile will exist within the range
  [`Byte.MinValue` (-128), `Byte.MaxValue` (127)].
- If attempting to convert between `CellTypes`, see
  [this note](./faq/#how-can-i-convert-a-tiles-celltype) on `CellType` conversions.

|             |     No NoData    |         Constant NoData        |        User Defined NoData        |
|-------------|:----------------:|:------------------------------:|:---------------------------------:|
| BitCells    | `BitCellType`    | N/A                            | N/A                               |
| ByteCells   | `ByteCellType`   | `ByteConstantNoDataCellType`   | `ByteUserDefinedNoDataCellType`   |
| UbyteCells  | `UByteCellType`  | `UByteConstantNoDataCellType`  | `UByteUserDefinedNoDataCellType`  |
| ShortCells  | `ShortCellType`  | `ShortConstantNoDataCellType`  | `ShortUserDefinedNoDataCellType`  |
| UShortCells | `UShortCellType` | `UShortConstantNoDataCellType` | `UShortUserDefinedNoDataCellType` |
| IntCells    | `IntCellType`    | `IntConstantNoDataCellType`    | `IntUserDefinedNoDataCellType`    |
| FloatCells  | `FloatCellType`  | `FloatConstantNoDataCellType`  | `FloatUserDefinedNoDataCellType`  |
| DoubleCells | `DoubleCellType` | `DoubleConstantNoDataCellType` | `DoubleUserDefinedNoDataCellType` |

The above table lists `CellType` `DataType`s in the leftmost column
and `NoData` policies along the top row.
A couple of points are worth
making here:

1. Bits are incapable of representing on, off, *and* some `NoData`
   value. As a consequence, there is no such thing as a Bit-backed tile
   which recognizes `NoData`.
2. While the types in the 'No NoData' and 'Constant NoData' are simply
   singleton objects that are passed around alongside tiles, the greater
   configurability of 'User Defined NoData' `CellType`s means that they
   require a constructor specifying the value which will count as
   `NoData`.

Let's look to how this information can be used:
```scala
/** Here's an array we'll use to construct tiles */
val myData = Array(42, 1, 2, 3)

/** The GeoTrellis-default integer CellType
 *   Note that it represents `NoData` values with the smallest signed
 *   integer possible with 32 bits (Int.MinValue or -2147483648).
 */
val defaultCT = IntConstantNoDataCellType
val normalTile = IntArrayTile(myData, 2, 2, defaultCT)

/** A custom, 'user defined' NoData CellType for comparison; we will
 *   treat 42 as NoData for this one rather than Int.MinValue
 */
val customCellType = IntUserDefinedNoDataValue(42)
val customTile = IntArrayTile(myData, 2, 2, customCellType)

/** We should expect that the first (default celltype) tile has the value 42 at (0, 0)
 *   This is because 42 is just a regular value (as opposed to NoData)
 *   which means that the first value will be delivered without surprise
 */
assert(normalTile.get(0, 0) == 42)
assert(normalTile.getDouble(0, 0) == 42.0)

/** Here, the result is less obvious. Under the hood, GeoTrellis is
 *   inspecting the value to be returned at (0, 0) to see if it matches our
 *   `NoData` policy and, if it matches (it does, we defined NoData as
 *   42 above), return Int.MinValue (no matter your underlying type, `get`
 *   on a tile will return an `Int` and `getDouble` will return a `Double`).
 *
 *   The use of Int.MinValue and Double.NaN is a result of those being the
 *   GeoTrellis-blessed values for NoData - below, you'll find a chart that
 *   lists all such values in the rightmost column
 */
assert(customTile.get(0, 0) == Int.MinValue)
assert(customTile.getDouble(0, 0) == Double.NaN)
```

A  point which is perhaps not intuitive is that `get` will *always*
return an `Int` and `getDouble` will *always* return a `Double`.
Representing NoData demands, therefore, that we map other celltypes'
`NoData` values to the native, default `Int` and `Double` `NoData`
values. `NoData` will be represented as `Int.MinValue` or `Double.Nan`.  


**Why you should care**

In most programming contexts, it isn't all that useful to think carefully
about the number of bits necessary to represent the data passed around
by a program. A program tasked with keeping track of all the birthdays
in an office or all the accidents on the New Jersey turnpike simply
doesn't benefit from carefully considering whether the allocation of
those extra few bits is *really* worth it. The costs for any lack of
efficiency are more than offset by the savings in development time and
effort. This insight - that computers have become fast enough for us to
be forgiven for many of our programming sins - is, by now, truism.

An exception to this freedom from thinking too hard about
implementation details is any software that tries, in earnest, to
provide the tools for reading, writing, and working with large arrays of
data. Rasters certainly fit the bill. Even relatively modest rasters
can be made up of millions of underlying cells. Additionally,
the semantics of a raster imply that each of these cells shares
an underlying data type. These points - that rasters are made up
of a great many cells and that they all share a backing
data type - jointly suggest that a decision regarding the underlying
data type could have profound consequences. More on these consequences
[below](#cell-type-performance).

Compliance with the GeoTIFF standard is another reason that management
of cell types is important for GeoTrellis. The most common format for
persisting a raster is the [GeoTIFF](https://trac.osgeo.org/geotiff/).
A GeoTIFF  is simply an array of data along with some useful tags
(hence the 'tagged' of 'tagged image file format'). One of these
tags specifies the size of each cell and how those bytes should be
interpreted (i.e. whether the data for a byte includes its
sign - positive or negative - or whether it counts up from 0 - and
is therefore said to be 'unsigned').

In addition to keeping track of the memory used by each cell in a `Tile`,
the cell type is where decisions about which values count as data (and
which, if any, are treated as `NoData`). A value recognized as `NoData`
will be ignored while mapping over tiles, carrying out focal operations
on them, interpolating for values in their region, and just about all of
the operations provided by GeoTrellis for working with `Tile`s.


**Cell Type Performance**

There are at least two major reasons for giving some thought to the
types of data you'll be working with in a raster: persistence and
performance.

Persistence is simple enough: smaller datatypes end up taking less space
on disk. If you're going to represent a region with only `true`/`false`
values on a raster whose values are `Double`s, 63/64 bits will be wasted.
Naively, this means somewhere around 63 times less data than if the most
compact form possible had been chosen (the use of `BitCells` would
be maximally efficient for representing the bivalent nature of boolean
values). See the chart below for a sense of the relative sizes of these
cell types.

The performance impacts of cell type selection matter in both a local
and a distributed (spark) context. Locally, the memory footprint will mean
that as larger cell types are used, smaller amounts of data can be held in
memory and worked on at a given time and that more CPU cache misses are to be
expected. This latter point - that CPU cache misses will increase - means that
more time spent shuffling data from the memory to the processor (which
is often a performance bottleneck). When running programs that
leverage spark for compute distribution, larger data types mean more
data to serialize and more data send over the (very slow, relatively
speaking) network.

In the chart below, `DataType`s are listed in the leftmost column and
important characteristics for deciding between them can be found to the
right. As you can see, the difference in size can be quite stark depending on
the cell type that a tile is backed by. That extra space is the price
paid for representing a larger range of values. Note that bit cells
lack the sufficient representational resources to have a `NoData` value.

|             | Bits / Cell | 512x512 Raster (mb) |     Range (inclusive)     |   GeoTrellis NoData Value    |
|-------------|:-----------:|---------------------|:-------------------------:|------------------------------|
| BitCells    | 1           | 0.032768            | [0, 1]                    |                         N/A  |
| ByteCells   | 8           | 0.262144            | [-128, 128]               |       -128 (`Byte.MinValue`) |
| UbyteCells  | 8           | 0.262144            | [0, 255]                  |                           0  |
| ShortCells  | 16          | 0.524288            | [-32768, 32767]           |    -32768 (`Short.MinValue`) |
| UShortCells | 16          | 0.524288            | [0, 65535]                |                           0  |
| IntCells    | 32          | 1.048576            | [-2147483648, 2147483647] | -2147483648 (`Int.MinValue`) |
| FloatCells  | 32          | 1.048576            | [-3.40E38, 3.40E38]       |                   Float.NaN  |
| DoubleCells | 64          | 2.097152            | [-1.79E308, 1.79E308]     |                  Double.NaN  |

One final point is worth making in the context of `CellType`
performance: the `Constant` types are able to depend upon macros which
inline comparisons and conversions. This minor difference can certainly
be felt while iterating through millions and millions of cells. If possible, Constant
`NoData` values are to be preferred. For convenience' sake, we've
attempted to make the GeoTrellis-blessed `NoData` values as unobtrusive
as possible a priori.

The limits of expected return types (discussed in the previous section) is used by
macros to squeeze as much speed out of the JVM as possible. Check out
[our macros docs](http://botanic.internal.azavea.com:8000/architecture/high-performance-scala/#macros)
for more on our use of macros like `isData` and `isNoData`.


Projections
===========

**What is a projection?**

In GIS, a *projection* is a mathematical transformation of
Latitude/Longitude coordinates on a sphere onto some other flat plane. Such
a plane is naturally useful for representing a map of the earth in 2D. A
projection is defined by a *Coordinate Reference System* (CRS), which holds
some extra information useful for reprojection. CRSs themselves have static
definitions, have agreed-upon string representations, and are usually made
public by standards bodies or companies. They can be looked up at
[SpatialReference.org](http://spatialreference.org/).

A *reprojection* is the transformation of coorindates in one CRS to another.
To do so, coordinates are first converted to those of a sphere. Every CRS
knows how to convert between its coordinates and a sphere's, so a
transformation `CRS.A -> CRS.B -> CRS.A` is actually `CRS.A -> Sphere ->
CRS.B -> Sphere -> CRS.A`. Naturally some floating point error does
accumulate during this process.


**Data structures:** `CRS`, `LatLng`, `WebMercator`, `ConusAlbers`

**Sources:** `geotrellis.proj4.{CRS, LatLng, WebMercator, ConusAlbers}`

Within the context of Geotrellis, the main projection-related object is the
`CRS` trait. It stores related `CRS` objects from underlying libraries, and
also provides the means for defining custom reprojection methods, should the
need arise. It's companion object provides convenience functions for
creating `CRS`s. Geotrellis currently has three `object`s that implement the
`CRS` trait: `LatLng`, `WebMercator`, and `ConusAlbers`.

**What can CRSs do?**

They can be transformed back into their String representations:

self => toWKT, toProj4String

**How are CRSs used throughout Geotrellis?**

`CRS`s are stored in the `*ProjecedExtent` classes and are used chiefly
to define how reprojections should operate. Example:

```scala
val wm = Line(...)  // A `LineString` vector object in WebMercator.
val ll: Line = wm.reproject(WebMercator, LatLng)  // The Line reprojected into LatLng.

```


Extents
=======


**Data structures:** `Extent`, `ProjectedExtent`, `TemporalProjectedExtent`,
`GridExtent`, `RasterExtent`

**Sources:** `geotrellis.vector.Extent`,
`geotrellis.vector.reproject.Reproject`,
`geotrellis.spark.TemporalProjectExtent`,
`geotrellis.raster.{ GridExtent, RasterExtent }`,
`geotrellis.raster.reproject.ReprojectRasterExtent`

**What is an extent?**

An `Extent` is a rectangular section of a 2D projection of the Earth. It is
represented by two coordinate pairs that are its "min" and "max" corners in
some Coorindate Reference System. "min" and "max" here are CRS
specific, as the location of the point `(0,0)` varies between different CRS.
An Extent can also be referred to as a *Bounding Box*.

Within the context of Geotrellis, the points within an `Extent` always
implicitely belong to some `CRS`, while a `ProjectedExtent` holds both the
original `Extent` and its current `CRS`. If you ever wish to reproject an
extent, you'd need the original `CRS` and hence a `ProjectedExtent`.

**What can Extents do?**

Extents can perform operations on themselves and other objects.

self => expansion, translation, reprojection

other: Extent => distance, intersection

other: Point => contains

**What are the other `*Extent` types?**

A `GridExtent` is any `Extent` which contains an extra internal grid. Grid
coordinates follow Graphics / Matrix conventions, where `(0,0)` is at the
top-left. The cells of this grid are usually larger than the individual
points of the underlying map.

A `GridExtent` specific to rasters, where the underlying map is some image
(possibly held in an `Array[Byte]`) is called a `RasterExtent`.
RasterExtents are used heavily in the `raster` subproject. Both `GridExtent`
and `RasterExtent` can be reprojected.

**How are Extents used throughout Geotrellis?**

Extents are held by `LayoutDefinition`s, which in turn are used heavily in
Raster reading, writing, and reprojection.

**How does reprojection work?**

Below is the rough call stack when projecting an `Extent`. It assumes you're
starting with a `ProjectExtent` so that the original `CRS` is available.

```
ProjectedExtent.reproject(CRS)
ReprojectExtent(Extent)  // implicit class wrapping
ReprojectExtent.reproject(CRS, CRS)
Reproject.apply(Extent, CRS, CRS)
Reproject.apply(Polygon, CRS, CRS)
Reproject.apply(Polygon, Transform(CRS, CRS))  // A transform is a function that translates a Point
                                               // via some inner `Transform` object, by default
                                               // a `BasicCoordinateTransform` from Proj4.
Polygon.apply(Reproject.apply(Line, Transform), Array[Line])  // Line is reprojected.
Polygon.envelope  // from `Geometry` trait
Geometry.jtsGeom.getEnvelopeInternal
Extent.jts2Extent(jts.geom.Envelope)  // implicitly. This is the final `Extent`.
```

*So basically*

`Extent => ReprojectExtent => Polygon => Line => (projected) Line => Polygon => jts.geom.Envelope => Extent`


Layout Definitions
====================

**Data structures:** `LayoutDefinition`, `TileLayout`, `CellSize`

**Sources:** `geotrellis.spark.tiling.LayoutDefinition`

**What is a Layout Definition?**

A Layout Definition describes the location, dimensions of, and organization
of a tiled area of a map. Conceptually, the tiled area forms a grid, and the
Layout Definitions describes that grid's area and cell width/height.

Within the context of Geotrellis, the `LayoutDefinition` class extends
`GridExtent`, and exposes methods for querying the sizes of the grid and
grid cells. Those values are stored in the `TileLayout` (the grid
description) and `CellSize` classes respectively. `LayoutDefinition`s are
used heavily during the raster reprojection process.

In essence, a `LayoutExtent` is the minimum information required to
describe some tiled map area in Geotrellis.

**How are Layout Definitions used throughout Geotrellis?**

They are used heavily when reading, writing, and reprojecting Rasters.

Map Algebra
===========

Map Algebra is a name given by Dr. Dana Tomlin in the 1980's to a way of
manipulating and transforming raster data. There is a lot of literature out
there, not least
[the book by the guy who "wrote the book" on map algebra](http://esripress.esri.com/display/index.cfm?fuseaction=display&websiteID=228&moduleID=0),
so we will only give a brief introduction here. GeoTrellis follows Dana's
vision of map algebra operations, although there are many operations that
fall outside of the realm of Map Algebra that it also supports.

Map Algebra operations fall into 3 general categories:

**Local Operations**

![Local Operations](./images/local-animations-optimized.gif localops)

Local operations are ones that only take into account the information of on
cell at a time. In the animation above, we can see that the blue and the
yellow cell are combined, as they are corresponding cells in the two tiles.
It wouldn't matter if the tiles were bigger or smaller - the only
information necessary for that step in the local operation is the cell
values that correspond to each other. A local operation happens for each
cell value, so if the whole bottom tile was blue and the upper tile were
yellow, then the resulting tile of the local operation would be green.

**Focal Operations**

![Focal Operations](./images/focal-animations.gif focalops)

Focal operations take into account a cell, and a neighborhood around that
cell. A neighborhood can be defined as a square of a specific size, or
include masks so that you can have things like circular or wedge-shaped
neighborhoods. In the above animation, the neighborhood is a 5x5 square
around the focal cell. The focal operation in the animation is a `focalSum`.
The focal value is 0, and all of the other cells in the focal neighborhood;
therefore the cell value of the result tile would be 8 at the cell
corresponding to the focal cell of the input tile. This focal operation
scans through each cell of the raster. You can imagine that along the
border, the focal neighborhood goes outside of the bounds of the tile; in
this case the neighborhood only considers the values that are covered by the
neighborhood. GeoTrellis also supports the idea of an analysis area, which
is the GridBounds that the focal operation carries over, in order to support
composing tiles with border tiles in order to support distributed focal
operation processing.

**Zonal Operations**

Zonal operations are ones that operate on two tiles: an input tile, and a
zone tile. The values of the zone tile determine what zone each of the
corresponding cells in the input tile belong to. For example, if you are
doing a `zonalStatistics` operation, and the zonal tile has a distribution
of zone 1, zone 2, and zone 3 values, we will get back the statistics such
as mean, median and mode for all cells in the input tile that correspond to
each of those zone values.

**How to use Map Algebra operations**

Map Algebra operations are defined as implicit methods on `Tile` or
`Traversable[Tile]`, which are imported with `import geotrellis.raster._`.

```scala
import geotrellis.raster._

val tile1: Tile = ???
val tile2: Tile = ???

// If tile1 and tile2 are the same dimensions, we can combine
// them using local operations

tile1.localAdd(tile2)

// There are operators for some local operations.
// This is equivalent to the localAdd call above

tile1 + tile2

// There is a local operation called "reclassify" in literature,
// which transforms each value of the function.
// We actually have a map method defined on Tile,
// which serves this purpose.

tile1.map { z => z + 1 } // Map over integer values.

tile2.mapDouble { z => z + 1.1 } // Map over double values.

tile1.dualMap({ z => z + 1 })({ z => z + 1.1 }) // Call either the integer value or double version, depending on cellType.

// You can also combine values in a generic way with the combine funciton.
// This is another local operation that is actually defined on Tile directly.

tile1.combine(tile2) { (z1, z2) => z1 + z2 }
```


The following packages are where Map Algebra operations are defined in
GeoTrellis:

- [`geotrellis.raster.local`](../../raster/src/main/scala/geotrellis/raster/mapalgebra/local)
defines operations which act on a cell without regard to its spatial
relations. Need to double every cell on a tile? This is the module you'll
want to explore.
- [`geotrellis.raster.focal`](../../raster/src/main/scala/geotrellis/raster/mapalgebra/focal)
defines operations which focus on two-dimensional windows (internally
referred to as neighborhoods) of a raster's values to determine their
outputs.
- [`geotrellis.raster.zonal`](../..raster/src/main/scala/geotrellis/raster/mapalgebra/zonal)
defines operations which apply over a zones as defined by corresponding cell
values in the zones raster.

[Conway's Game of Life](http://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)
can be seen as a focal operation in that each cell's value depends on
neighboring cell values. Though focal operations will tend to look at a
local region of this or that cell, they should not be confused with the
operations which live in `geotrellis.raster.local` - those operations
describe transformations over tiles which, for any step of the calculation,
need only know the input value of the specific cell for which it is
calculating an output (e.g. incrementing each cell's value by 1).
