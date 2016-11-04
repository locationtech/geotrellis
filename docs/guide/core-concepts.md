Geographical Information Systems (GIS), like any specialized field, has a wealth
of jargon and unique concepts. When represented in software, these concepts
can sometimes be skewed or expanded from their original forms. We give a thorough definition of many of the core concepts
here, while referencing the Geotrellis objects and source files backing them.

This document aims to be informative to new and experienced GIS users alike.
If GIS is brand, brand new to you, [this document](https://www.gislounge.com/what-is-gis/) is
 a useful high level overview.

Raster Data
===========

Vector Data
===========

Vector Tiles
===========

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
