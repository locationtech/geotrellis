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

  def `Performing a count of points that lie in regular polygon grid`: Unit = {
    import geotrellis.raster._
    import geotrellis.spark._
    import geotrellis.spark.tiling._
    import geotrellis.vector._

    import org.apache.spark.HashPartitioner
    import org.apache.spark.rdd.RDD

    import java.util.UUID

    // see https://stackoverflow.com/questions/47359243/geotrellis-get-the-points-that-fall-in-a-polygon-grid-rdd

    val pointValueRdd : RDD[Feature[Point,Double]] = ???
    val squareGridRdd : RDD[Polygon] = ???

    // Since we'll be grouping by key and then joining, it's work defining a partitioner
    // up front.
    val partitioner = new HashPartitioner(100)

    // First, we'll determine the bounds of the Polygon grid
    // and the average height and width, to create a GridExtent
    val (extent, totalHeight, totalWidth, totalCount) =
      squareGridRdd
        .map { poly =>
          val e = poly.envelope
          (e, e.height, e.width, 1)
        }
        .reduce { case ((extent1, height1, width1, count1), (extent2, height2, width2, count2)) =>
          (extent1.combine(extent2), height1 + height2, width1 + width2, count1 + count2)
        }

    val gridExtent = GridExtent(extent, totalHeight / totalCount, totalWidth / totalCount)

    // We then use this for to construct a LayoutDefinition, that represents "tiles"
    // that are 1x1.
    val layoutDefinition = LayoutDefinition(gridExtent, tileCols = 1, tileRows = 1)

    // Now that we have a layout, we can cut the polygons up per SpatialKey, as well as
    // assign points to a SpatialKey, using ClipToGrid

    // In order to keep track of which polygons go where, we'll assign a UUID to each
    // polygon, so that they can be reconstructed. If we were dealing with PolygonFeatures,
    // we could store the feature data as well. If those features already had IDs, we could
    // also just use those IDs instead of UUIDs.
    // We'll also store the original polygon, as they are not too big and it makes
    // the reconstruction process (which might be prone to floating point errors) a little
    // easier. For more complex polygons this might not be the most performant strategy.
    // We then group by key to produce a set of polygons that intersect each key.
    val cutPolygons: RDD[(SpatialKey, Iterable[Feature[Geometry, (Polygon, UUID)]])] =
      squareGridRdd
        .map { poly => Feature(poly, (poly, UUID.randomUUID)) }
        .clipToGrid(layoutDefinition)
        .groupByKey(partitioner)

    // While we could also use clipToGrid for the points, we can
    // simply use the mapTransform on the layout to determien what SpatialKey each
    // point should be assigned to.
    // We also group this by key to produce the set of points that intersect the key.
    val pointsToKeys: RDD[(SpatialKey, Iterable[PointFeature[Double]])] =
      pointValueRdd
        .map { pointFeature =>
          (layoutDefinition.mapTransform.pointToKey(pointFeature.geom), pointFeature)
        }
        .groupByKey(partitioner)

    // Now we can join those two RDDs and perform our point in polygon tests.
    // Use a left outer join so that polygons with no points can be recorded.
    // Once we have the point information, we key the RDD by the UUID and
    // reduce the results.
    val result: RDD[Feature[Polygon, Double]] =
      cutPolygons
        .leftOuterJoin(pointsToKeys)
        .flatMap { case (_, (polyFeatures, pointsOption)) =>
          pointsOption match {
            case Some(points) =>
              for(
                Feature(geom, (poly, uuid)) <- polyFeatures;
                Feature(point, value) <- points if geom.intersects(point)
              ) yield {
                (uuid, Feature(poly, (value, 1)))
              }
            case None =>
              for(Feature(geom, (poly, uuid)) <- polyFeatures) yield {
                (uuid, Feature(poly, (0.0, 0)))
              }
          }
        }
        .reduceByKey { case (Feature(poly1, (accum1, count1)), Feature(poly2, (accum2, count2))) =>
          Feature(poly1, (accum1 + accum2, count1 + count2))
        }
        .map { case (_, feature) =>
          // We no longer need the UUID; also compute the mean
          feature.mapData { case (acc, c) => acc / c }
        }

  }
}
