#geotrellis.vector
##Features and Geometries

In addition to working with raster data, Geotrellis provides a number of
facilities for the creation, representation, and modification of vector
data. The data types central to this functionality (`geotrellis.vector.Feature`
and `geotrellis.vector.Geometry`) correspond - and not by accident - to
certain objects found in [the GeoJson spec](http://geojson.org/geojson-spec.html).
`Feature`s correspond to the objects listed  under `features` in a geojson
`FeatureCollection`. `Geometry`s, to `geometries` in a geojson `Feature`.

##Geometries
The base geometry class can be found in Geometry.scala. Concrete
geometries include:
+ `geotrellis.vector.Point`
+ `geotrellis.vector.MultiPoint`
+ `geotrellis.vector.Line`
+ `geotrellis.vector.MultiLine`
+ `geotrellis.vector.Polygon`
+ `geotrellis.vector.MultiPolygon`
+ `geotrellis.vector.GeometryCollection`

Working with these geometries is a relatively straightforward affair.
Let's take a look:
```scala
import geotrellis.vector._
/*
 * First, let's create a Point. Then, we'll use its intersection method.
 * We should also use intersection's alias '&' for good measure.
 */
val myPoint = Point(1.0, 1.1) // Create a point
val selfIntersection = myPoint intersection Point(1.0, 1.1) // Intersection method
val nonIntersection = myPoint & Point(200, 300) // Intersection alias
```
At this point, the values `selfIntersection` and `nonIntersection` are
`GeometryResult` containers. These containers are what many JTS
operations on `Geometry` objects will wrap their results in. To
idiomatically destructure these wrappers, we do the following:
```scala
def unwrapPoint(res: PointGeometryIntersectionResult): Option[Point] =
  res match {
    case PointResult(point) => Some(point)
    case _ => None
  }
// Note:
assert(unwrapPoint(selfIntersection) == Some(myPoint))
assert(unwrapPoint(nonIntersection) == None)
```

Beyond the methods which come with any `Geometry` object there are
implicits in many geotrellis modules which will extend Geometry
capabilities. For instance, after importing `geotrellis.vector.json._`,
it becomes possible to call the `toGeoJson` method on any `Geometry`:
```scala
import geotrellis.vector.json._
assert(Point(1,1).toGeoJson == """{"type":"Point","coordinates":[1.0,1.0]}""")
```

Methods which are specific to certain subclasses of `Geometry` exist
too. For example, `geotrellis.vector.MultiLine` is extended by
`geotrellis.vector.op` such that this becomes possible:
```scala
import geotrellis.vector.op._
val myML = MultiLine.EMPTY
myML.unionGeometries
```

The following packages extend `Geometry` capabilities:
- [geotrellis.vector.op](op/)
- [geotrellis.vector.io.WKB](io/WKB/)
- [geotrellis.vector.io.WKT](io/WKT/)
- [geotrellis.vector.io.json](io/json/)
- [geotrellis.vector.reproject](reproject/)

##Features

TODO: Write this

