package geotrellis.doc.examples.vector

object VectorExamples {
  def `Writing a sequence of vector data to a GeoJson feature collection`: Unit = {
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
  }
}
