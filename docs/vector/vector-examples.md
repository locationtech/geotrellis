# Examples

### Writing a sequence of vector data to a GeoJson feature collection

One common thing to go wrong with writing feature data using the `.toGeoJson`
method is not having the implicit `JsonWriter` (or `JsonFormat`) for the
feature data type in scope. For example, in the below example, if we
get rid of teh `import spray.json.DefaultJsonProtocol._` import statement,
we lose the `JsonFormat[Int]` hat it provides, and so the implicit class
that has the `toGeoJson` that can be called on the `features` value no longer has
the context bounds satisfied, and you'll get and error like

```bash
[error] /geotrellis/doc-examples/src/main/scala/geotrellis/doc/examples/spark/VectorExamples.scala:16: value toGeoJson is not a member of List[geotrellis.vector.PolygonFeature[Int]]
[error]     val geojson: String = features.toGeoJson
[error]                                    ^
[error] one error found
```

With the correct imports, though, everything compiles fine. So always make sure to have all JsonFormat, JsonReader or JsonWriter
implicits available when working with spray-json!

Here is the working example:

```scala
import geotrellis.vector._
import geotrellis.vector.io._

// This import is important: otherwise the JsonFormat for the
// feature data type is not available (the feature data type being Int)
import spray.json.DefaultJsonProtocol._

// Starting with a list of polygon features,
// e.g. the return type of tile.toVector
val features: List[PolygonFeature[Int]] = ???

// Because we've imported geotrellis.vector.io, we get
// GeoJson methods implicitly added to vector types,
// including any Traversable[Feature[G, D]]

val geojson: String = features.toGeoJson

println(geojson)
```
