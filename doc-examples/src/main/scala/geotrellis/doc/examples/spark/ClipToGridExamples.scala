package geotrellis.doc.examples.distance

object ClipToGridExamples {
  def `Performing a polygonal sum from an RDD of Polygons to GeoTiffs on S3`: Unit = {
    import geotrellis.raster._
    import geotrellis.spark._
    import geotrellis.spark.tiling._
    import geotrellis.vector._

    import org.apache.spark.HashPartitioner
    import org.apache.spark.rdd.RDD

    import java.net.URI
    import java.util.UUID

    // The extends of the GeoTiffs, along with the URIs
    val geoTiffUris: RDD[Feature[Polygon, URI]] = ???
    val polygons: RDD[Feature[Polygon, UUID]] = ???

    // Choosing the appropriately resolute layout for the data is here considered a client concern.
    val layout: LayoutDefinition = ???

    // Abbreviation for the code to read the window of the GeoTiff off of S3
    def read(uri: URI, window: Extent): Raster[Tile] = ???

    val groupedPolys: RDD[(SpatialKey, Iterable[MultiPolygonFeature[UUID]])] =
      polygons
        .clipToGrid(layout)
        .flatMap { case (key, feature) =>
          val mpFeature: Option[MultiPolygonFeature[UUID]] =
            feature.geom match {
              case p: Polygon => Some(feature.mapGeom(_ => MultiPolygon(p)))
              case mp: MultiPolygon => Some(feature.mapGeom(_ => mp))
              case _ => None
            }
          mpFeature.map { mp => (key, mp) }
        }
        .groupByKey(new HashPartitioner(1000))

    val rastersToKeys: RDD[(SpatialKey, URI)] =
      geoTiffUris
        .clipToGrid(layout)
        .flatMap { case (key, feature) =>
          // Filter out any non-polygonal intersections.
          // Also, we will do the window read from the SpatialKey extent, so throw out polygon.
          feature.geom match {
            case p: Polygon => Some((key, feature.data))
            case mp: MultiPolygon => Some((key, feature.data))
            case _ => None
          }
        }

    val joined: RDD[(SpatialKey, (Iterable[MultiPolygonFeature[UUID]], URI))] =
      groupedPolys
        .join(rastersToKeys)

    val totals: Map[UUID, Long] =
      joined
        .flatMap { case (key, (features, uri)) =>
          val raster = read(uri, layout.mapTransform.keyToExtent(key))

          features.map { case Feature(mp, uuid) =>
            (uuid, raster.tile.polygonalSum(raster.extent, mp).toLong)
          }
        }
        .reduceByKey(_ + _)
        .collect
        .toMap
  }
}
