package geotrellis.doc.examples.spark

object PolygonalSummaryExamples {
  def `Performing a polygonal sum from an RDD of Polygons to GeoTiffs on S3`: Unit = {
    import geotrellis.raster._
    import geotrellis.raster.summary.polygonal._
    import geotrellis.raster.summary.polygonal.visitors.SumVisitor
    import geotrellis.raster.summary.types.SumValue
    import geotrellis.layer.SpatialKey
    import geotrellis.spark._
    import geotrellis.store._
    import geotrellis.store.file._
    import geotrellis.spark.store.file.cog.FileCOGLayerReader
    import geotrellis.spark.summary.polygonal._
    import geotrellis.vector.{Feature, Geometry}


    import org.apache.spark.SparkContext
    import org.apache.spark.rdd.RDD

    implicit val sc: SparkContext = ???

    // Set as the max zoom of the dataset you're reading from
    val zoom: Int = ???
    // Create the attributes store that contains information about catalog.
    val attributeStore = FileAttributeStore("/path/to/catalog")
    // Create the reader instance to query tiles stored as a Structured COG Layer
    val reader = FileCOGLayerReader(attributeStore)
    // Read TileLayerRDD
    val rasterRdd: TileLayerRDD[SpatialKey] = reader.read[SpatialKey, Tile](LayerId("example_cog_layer", zoom))

    val polygons: List[Geometry] = ???

    // summaryRdd holds polygonal summary results in the Feature data property
    val summaryRdd: RDD[Feature[Geometry, PolygonalSummaryResult[SumValue]]] =
      rasterRdd.polygonalSummary(polygons, SumVisitor, RasterizerOptions.DEFAULT)
  }
}
