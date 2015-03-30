#geotrellis.vector
##Features and Geometries

In addition to working with raster data, Geotrellis provides a number of facilities for the creation, representation, and modification of vector data. The data types central to this functionality (`geotrellis.vector.Feature` and `geotrellis.vector.Geometry`) correspond - and not by accident - to certain objects found in [the GeoJson spec](http://geojson.org/geojson-spec.html).
`Feature`s correspond to the objects listed  under `features` in a geojson `FeatureCollection`. `Geometry`s, to `geometries` in a geojson `Feature`.

##Geometries
The base `Geometry` class can be found in `Geometry.scala`. Concrete geometries include:
+ `geotrellis.vector.Point`
+ `geotrellis.vector.MultiPoint`
+ `geotrellis.vector.Line`
+ `geotrellis.vector.MultiLine`
+ `geotrellis.vector.Polygon`
+ `geotrellis.vector.MultiPolygon`
+ `geotrellis.vector.GeometryCollection`

Working with these geometries is a relatively straightforward affair. Let's take a look:
```scala
import geotrellis.vector._
/*
 * First, let's create a Point. Then, we'll use its intersection method.
 * Note: we are also using intersection's alias '&'.
 */
val myPoint = Point(1.0, 1.1) // Create a point
val selfIntersection = myPoint intersection Point(1.0, 1.1) // Intersection method
val nonIntersection = myPoint & Point(200, 300) // Intersection alias
```
At this point, the values `selfIntersection` and `nonIntersection` are `GeometryResult` containers. These containers are what many JTS operations on `Geometry` objects will wrap their results in. Source for the To idiomatically destructure these wrappers, we do the following:
```scala
def unwrapPoint(res: PointGeometryIntersectionResult): Option[Point] =
  res match {
    case PointResult(point) => Some(point)
    case _ => None
  }
// Et voila:
assert(unwrapPoint(selfIntersection) == Some(myPoint))  // Either some point
assert(unwrapPoint(nonIntersection) == None)  // Or nothing at all
```

Beyond the methods which come with any `Geometry` object there are implicits in many geotrellis modules which will extend Geometry capabilities. For instance, after importing `geotrellis.vector.io.json._`, it becomes possible to call the `toGeoJson` method on any `Geometry`:
```scala
import geotrellis.vector.io.json._
assert(Point(1,1).toGeoJson == """{"type":"Point","coordinates":[1.0,1.0]}""")
```
If you need to move from a geometry to a serialized representation or vice-versa, take a look at the `io` directory's contents. This naming convention for input and output is common throughout Geotrellis. So if you're trying to get spatial representations in or out of your program, spend some time seeing if the problem has already been solved.

Methods which are specific to certain subclasses of `Geometry` exist too. For example, `geotrellis.vector.MultiLine` is implicitly extended by `geotrellis.vector.op` such that this becomes possible:
```scala
import geotrellis.vector.op._
val myML = MultiLine.EMPTY
myML.unionGeometries
```

The following packages extend `Geometry` capabilities:
- [geotrellis.vector.affine](affine/)
- [geotrellis.vector.io.json](io/json/)
- [geotrellis.vector.io.WKT](io/WKT/)
- [geotrellis.vector.io.WKB](io/WKB/)
- [geotrellis.vector.op](op/)
- [geotrellis.vector.reproject](reproject/)

##Features
The `Feature` class is odd. At first blush, it thinly wraps one of the afforementioned `Geometry` objects along with some type of data. Its purpose will be clear if you can keep in mind the importance of the geojson format of serialization which is now ubiquitous in the GIS software space. It can be found in `Feature.scala`.

Let's examine some source code so that this is all a bit clearer.
From `geotrellis.vector.Feature.scala`:
```scala
abstract class Feature[D] {
  type G <: Geometry
  val geom: G ; val data: D
}

case class PointFeature[D](geom: Point, data: D) extends Feature[D] {type G = Point}
```
These type signatures tell us a good deal. Let's make this easy on our brains and put our findings into a list.
- The type `G` is [some instance or other](http://docs.scala-lang.org/tutorials/tour/upper-type-bounds.html) of `Geometry` (which we explored just above).
- The value, `geom`, which anything the compiler recognizes as a `Feature` must make available in its immediate closure must be of type `G`.
- As with `geom` the compiler will not be happy unless a `Feature` provides `data`.
- Whereas, with `geom`, we could say a good deal about the types of stuff (only things we call geometries) that would satisfy the compiler, we have nothing in particular to say about `D`.

Our difficulty with `D` is compounded by the fact that the `Point`-focused feature, `PointFeature` makes good on `geom` by using `Point` (which is one of the concrete instances of `Geometry` introduced above) while telling us nothing at all about `data`'s type.
Let's look at some code which does something with D (code which calls one of D's methods) so that we know what to expect. Remember: types are just contracts which the compiler is kind enough to enforce for us. In well-written code, types (and type variables!) can tell us a great deal about what was in the head of the author.
There's only one package which does anything with `D`, so our job should be relatively easy. From `geotrellis.vector.io.json.FeatureFormats.scala`:
```Scala
def writeFeatureJson[D: JsonWriter](obj: Feature[D]): JsValue = {
  JsObject(
    "type" -> JsString("Feature"),
    "geometry" -> GeometryFormat.write(obj.geom),
    "properties" -> obj.data.toJson
  )
}
def readFeatureJson[D: JsonReader, G <: Geometry: JsonReader, F <: Feature[D]](value: JsValue)(create : (G, D) => F): F = {
  value.asJsObject.getFields("type", "geometry", "properties") match {
    case Seq(JsString("Feature"), geom, data) =>
      val g = geom.convertTo[G]
      val d = data.convertTo[D]
      create(g,d)
    case _ => throw new DeserializationException("Feature expected")
  }
}
```
Right away, `D`'s purpose is clear: any `D` which comes with the tools necessary for json serialization and deserialization will suffice. In effect, `data` corresponds to the "properties" member of the geojson spec's Feature object.
If you can provide the serialization tools (almost certainly implicit conversions between some case class and [spray json](https://github.com/spray/spray-json)), the `Feature` object in `geotrellis.vector` does the heavy lifting of embedding your (thus serializable) data into the larger structure which includes a geometry. There's even support for geojson IDs: the "ID" member of a geojson Feature is represented by the keys of a `Map` from `String` to `Feature[D]`. Data in both the ID and non-ID variants of geojson Feature formats is easily transformed.

###Submodules
These submodules define useful methods for dealing with the denizens of `geotrellis.vector`:
`geotrellis.vector.io` defines input/output (serialization) of geometries
`geotrellis.vector.op` defines common operations on geometries
`geotrellis.vector.reproject` defines methods for translating between projections
`geotrellis.vector.affine` defines transformations which preserve collinearity
