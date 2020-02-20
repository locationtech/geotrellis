/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.doc.examples.distance

object ClipToGridExamples {
  def `Performing a count of points that lie in regular polygon grid`: Unit = {
    import geotrellis.raster._
    import geotrellis.layer.{SpatialKey, LayoutDefinition}
    import geotrellis.spark._
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
          val e = poly.extent
          (e, e.height, e.width, 1)
        }
        .reduce { case ((extent1, height1, width1, count1), (extent2, height2, width2, count2)) =>
          (extent1.combine(extent2), height1 + height2, width1 + width2, count1 + count2)
        }

    val gridExtent = GridExtent[Long](extent, CellSize(totalHeight / totalCount, totalWidth / totalCount))

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
