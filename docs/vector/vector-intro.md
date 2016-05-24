# geotrellis.vector

>"Raster is faster but vector is correcter."
— Somebody

## Features and Geometries

In addition to working with raster data, Geotrellis provides
a number of tools for the creation, representation, and
modification of vector data. The data types central to this
functionality (`geotrellis.vector.Feature` and
`geotrellis.vector.Geometry`) correspond - and not by accident -
to certain objects found in [the GeoJson
spec](http://geojson.org/geojson-spec.html). `Feature`s
correspond to the objects listed  under `features` in a
geojson `FeatureCollection`. `Geometry`s, to `geometries`
in a geojson `Feature`.

## Geometries

The base `Geometry` class can be found in `Geometry.scala`.
Concrete geometries include:
- `geotrellis.vector.Point`
- `geotrellis.vector.MultiPoint`
- `geotrellis.vector.Line`
- `geotrellis.vector.MultiLine`
- `geotrellis.vector.Polygon`
- `geotrellis.vector.MultiPolygon`
- `geotrellis.vector.GeometryCollection`

Working with these geometries is a relatively straightforward
affair. Let's take a look:

```scala
import geotrellis.vector._

/** First, let's create a Point. Then, we'll use its intersection method.
  * Note: we are also using intersection's alias '&'.
  */
val myPoint = Point(1.0, 1.1) // Create a point
// Intersection method
val selfIntersection = myPoint intersection Point(1.0, 1.1)
// Intersection alias
val nonIntersection = myPoint & Point(200, 300)
```

At this point, the values `selfIntersection` and `nonIntersection`
are `GeometryResult` containers. These containers are what many JTS
operations on `Geometry` objects will wrap their results in.
To idiomatically destructure these wrappers, we can use the
`as[G <: Geometry]` function which either returns `Some(G)` or `None`.

```scala
val pointIntersection = (Point(1.0, 2.0) & Point(1.0, 2.0)).as[Point]
val pointNonIntersection = (Point(1.0, 2.0) & Point(12.0, 4.0)).as[Point]

assert(pointIntersection == Some(Point(1.0, 2.0)))  // Either some point
assert(pointNonIntersection == None)                // Or nothing at all
```

As convenient as `as[G <: Geometry]` is, it offers no guarantees about
the domain over which it ranges. So, while you can expect a neatly
packaged `Option[G <: Geometry]`, it isn't necessarily the case that the
`GeometryResult` object produced by a given set of operations is
possibly convertable to the `Geometry` subtype you choose. For example,
a `PointGeometryIntersectionResult.as[Polygon]` will *always* return
`None`.

An alternative approach uses pattern matching and ensures an exhaustive
check of the results.
[`Results.scala`](../../vector/src/main/scala/geotrellis/vector/Results.scala)
contains a large
[ADT](https://en.wikipedia.org/wiki/Algebraic_data_type) which encodes
the possible outcomes for different types of outcomes. The result type of
a JTS-dependent vector operation can be found somewhere on this tree to
the effect that an exhaustive match can be carried out to determine the
`Geometry` (excepting cases of `NoResult`, for which there is no
`Geometry`).

For example, we note that a `Point`/`Point` intersection has
the type `PointOrNoResult`. From this we can deduce that it is either a
`Point` underneath or else nothing:

```scala
scala> import geotrellis.vector._
scala> p1 & p2 match {
     |   case PointResult(_) => println("A Point!)
     |   case NoResult => println("Sorry, no result.")
     | }
A Point!
```

Beyond the methods which come with any `Geometry` object there are
implicits in many geotrellis modules which will extend Geometry
capabilities. For instance, after importing `geotrellis.vector.io._`,
it becomes possible to call the `toGeoJson` method on any `Geometry`:

```scala
import geotrellis.vector.io._
assert(Point(1,1).toGeoJson == """{"type":"Point","coordinates":[1.0,1.0]}""")
```

If you need to move from a geometry to a serialized representation or
vice-versa, take a look at the `io` directory's contents. This naming
convention for input and output is common throughout Geotrellis.
So if you're trying to get spatial representations in or out of your
program, spend some time seeing if the problem has already been solved.

Methods which are specific to certain subclasses of `Geometry` exist too.
For example, `geotrellis.vector.MultiLine` is implicitly extended by
`geotrellis.vector.op` such that this becomes possible:

```scala
import geotrellis.vector.op._
val myML = MultiLine.EMPTY
myML.unionGeometries
```

The following packages extend `Geometry` capabilities:
- [geotrellis.vector.io.json](io/json/)
- [geotrellis.vector.io.WKT](io/WKT/)
- [geotrellis.vector.io.WKB](io/WKB/)
- [geotrellis.vector.op](op/)
- [geotrellis.vector.op.affine](op/affine/)
- [geotrellis.vector.reproject](reproject/)

## Features
The `Feature` class is odd. At first blush, it thinly wraps one of the
afforementioned `Geometry` objects along with some type of data. Its
purpose will be clear if you can keep in mind the importance of the
geojson format of serialization which is now ubiquitous in the GIS
software space. It can be found in `Feature.scala`.

Let's examine some source code so that this is all a bit clearer.
From `geotrellis.vector.Feature.scala`:

```scala
abstract class Feature[D] {
  type G <: Geometry
  val geom: G ; val data: D
}

case class PointFeature[D](geom: Point, data: D) extends Feature[D] {type G = Point}
```
These type signatures tell us a good deal. Let's make this easy
on ourselves and put our findings into a list.
- The type `G` is [some instance or other](http://docs.scala-lang.org/tutorials/tour/upper-type-bounds.html)
of `Geometry` (which we explored just above).
- The value, `geom`, which anything the compiler recognizes as a
`Feature` must make available in its immediate closure must be of type `G`.
- As with `geom` the compiler will not be happy unless a `Feature` provides `data`.
- Whereas, with `geom`, we could say a good deal about the types of
  stuff (only things we call geometries) that would satisfy the compiler,
  we have nothing in particular to say about `D`.

Our difficulty with `D` is shared by the `Point`-focused feature,
`PointFeature`. `PointFeature` uses `Point` (which is one of the concrete instances
of `Geometry` introduced above) while telling us nothing at all about `data`'s
type. This is just sugar for passing around a `Point` and some
associated metadata.

Let's look at some code which does something with D (code which calls
one of D's methods) so that we know what to expect. Remember: types are
just contracts which the compiler is kind enough to enforce. In
well-written code, types (and type variables) can tell us a great deal
about what was in the head of the author.

There's only one `package` which does anything with `D`, so the
constraints (and our job) should be relatively easy.
In [`geotrellis.vector.io.json.FeatureFormats.scala`](../../vector/src/main/scala/vector/io/json/FeatureFormats.scala)
there are `ContextBound`s on `D` which ensure that they have JsonReader,
JsonWriter, and JsonFormat implicits available (this is a
[typeclass](http://danielwestheide.com/blog/2013/02/06/the-neophytes-guide-to-scala-part-12-type-classes.html),
and it allows for something like type-safe duck-typing).

`D`'s purpose is clear enough: any `D` which comes with the tools
necessary for json serialization and deserialization will suffice.
In effect, `data` corresponds to the "properties" member of the
geojson spec's `Feature` object.

If you can provide the serialization tools (that is, implicit
conversions between some type (your `D`) and [spray json](https://github.com/spray/spray-json)),
the `Feature` object in `geotrellis.vector` does the heavy lifting
of embedding your (thus serializable) data into the larger structure
which includes a `Geometry`. There's even support for geojson IDs: the
"ID" member of a geojson Feature is represented by the keys of a `Map`
from `String` to `Feature[D]`. Data in both the ID and non-ID variants
of geojson Feature formats is easily transformed.

## GeoTrellis Extents

There's one more piece to the `geotrellis.vector` puzzle: `Extent`.
A `geotrellis.vector.Extent` is nothing more than a rectangular
polygon on projection. This is useful mainly as a tool for defining the
extent covered by a tile - the two jointly yield a raster (for more
on rasters, go [here](../../raster/src/main/scala/geotrellis/raster)).
Constructing `Extent`s is easy. Since they're rectangles,
we only need to provide four unique values. Take a look at the
[source](../../vector/src/main/scala/geotrellis/vector/Extent.scala) for
more.

Pay special attention to `ProjectedExtent` if you need your geometries
to be projection-aware. Really, that's about all you need to know to get
started with extents. They're a powerful tool for a tightly defined task.

From Extent.scala:

```scala
case class ProjectedExtent(extent: Extent, crs: CRS) {
  def reproject(dest: CRS): Extent = extent.reproject(crs, dest)
}
```

Really, that's about all you need to know to get started with
extents. They're a powerful tool for a tightly defined task.

## Submodules

These submodules define useful methods for dealing with
the entities that call `geotrellis.vector` home:
- `geotrellis.vector.io` defines input/output (serialization) of geometries
- `geotrellis.vector.op` defines common operations on geometries
- `geotrellis.vector.reproject` defines methods for translating between projections
