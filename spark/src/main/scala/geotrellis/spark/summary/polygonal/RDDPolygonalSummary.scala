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

package geotrellis.spark.summary.polygonal

import java.util.UUID

import cats.Semigroup
import cats.syntax.semigroup._
import geotrellis.layer.{Metadata, SpatialKey, TileLayerMetadata}
import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.summary.GridVisitor
import geotrellis.raster.summary.polygonal._
import geotrellis.util.MethodExtensions
import geotrellis.vector._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object RDDPolygonalSummary {
  /**
    * Perform a polygonal summary over all geometries in geometryRdd using data from rasterRdd
    *
    * Helper methods are provided in [[RDDPolygonalSummaryMethods]] which handle some common
    * derived cases, such as only performing a summary for a single geometry across rasterRdd.
    *
    * The provided visitor is cloned for each individual polygonal summary operation on each
    * geometry in the input. Do not rely on private state within the passed visitor to persist
    * to each summary operation.
    *
    * @param rasterRdd
    * @param geometryRdd
    * @param visitor
    * @param options Rasterizer.Options for this polygonal summary operation
    * @param semigroupR
    * @param tagR
    * @tparam R Type of the summary being performed
    * @tparam T Type of the Raster contained in rasterRdd
    * @return An RDD containing each unique input geometry and it's summary result R
    */
  def apply[R, T <: CellGrid[Int]](
      rasterRdd: RDD[(SpatialKey, T)] with Metadata[TileLayerMetadata[SpatialKey]],
      geometryRdd: RDD[Geometry],
      visitor: GridVisitor[Raster[T], R],
      options: Rasterizer.Options
  )(implicit semigroupR: Semigroup[R],
    tagR: ClassTag[R]): RDD[Feature[Geometry, PolygonalSummaryResult[R]]] = {
    val layout = rasterRdd.metadata.layout

    val keyedGeometryRdd: RDD[(SpatialKey, Feature[Geometry, UUID])] =
      geometryRdd.flatMap { geom =>
        val uuid = UUID.randomUUID
        val keys: Set[SpatialKey] = layout.mapTransform.keysForGeometry(geom)
        keys.map(k => (k, Feature(geom, uuid)))
      }

    // Combine geometries and rasters by SpatialKey, then loop partitions
    //  and perform the polygonal summary on each combination
    val joinedRdd = keyedGeometryRdd.join(rasterRdd)
    val featuresWithSummaries: RDD[(UUID, Feature[Geometry, PolygonalSummaryResult[R]])] =
      joinedRdd.map { data => {
        val spatialKey = data._1
        val feature: Feature[Geometry, UUID] = data._2._1
        val tile: T = data._2._2
        val extent: Extent = layout.mapTransform.keyToExtent(spatialKey)
        val raster: Raster[T] = Raster(tile, extent)
        val result: PolygonalSummaryResult[R] =
          raster.polygonalSummary(feature.geom, visitor.getClass.newInstance, options)
        (feature.data, Feature(feature.geom, result))
      }
    }

    // Geometries may have been split across SpatialKey leading to multiple UUID keys.
    // Combine by UUID and then drop the UUID, which was added within this method
    featuresWithSummaries
      .reduceByKey { (a, b) => Feature(a.geom, a.data.combine(b.data)) }
      .map { _._2 }
  }

  trait RDDPolygonalSummaryMethods[R, T <: CellGrid[Int]]
      extends MethodExtensions[RDD[(SpatialKey, T)] with Metadata[TileLayerMetadata[SpatialKey]]] {
    /**
     * @see [[RDDPolygonalSummary]]
     */
    def polygonalSummary[R](geometryRdd: RDD[Geometry],
                            visitor: GridVisitor[Raster[T], R],
                            options: Rasterizer.Options)(
        implicit semigroupR: Semigroup[R], tagR: ClassTag[R]): RDD[Feature[Geometry, PolygonalSummaryResult[R]]] = {
      RDDPolygonalSummary(self, geometryRdd, visitor, options)
    }

    /**
     * Helper method that automatically lifts Seq[Geometry] into RDD[Geometry]
     *
     * @see [[RDDPolygonalSummary]]
     */
    def polygonalSummary[R](geometries: Seq[Geometry],
                            visitor: GridVisitor[Raster[T], R],
                            options: Rasterizer.Options)(
        implicit semigroupR: Semigroup[R], tagR: ClassTag[R]): RDD[Feature[Geometry, PolygonalSummaryResult[R]]] = {
      self.polygonalSummary(self.sparkContext.parallelize(geometries), visitor, options)
    }

    /**
     * Helper method that automatically lifts Seq[Geometry] into RDD[Geometry]
     * and uses the default Rasterizer.Options
     *
     * @see [[RDDPolygonalSummary]]
     */
    def polygonalSummary[R](geometries: Seq[Geometry],
                            visitor: GridVisitor[Raster[T], R])(
        implicit semigroupR: Semigroup[R], tagR: ClassTag[R]): RDD[Feature[Geometry, PolygonalSummaryResult[R]]] = {
      self.polygonalSummary(self.sparkContext.parallelize(geometries),
                            visitor,
                            PolygonalSummary.DefaultOptions)
    }

    /**
     * Helper method for performing a polygonal summary across a raster RDD
     * that takes only a single Geometry and returns a single PolygonalSummaryResult[R]
     * for that Geometry.
     *
     * @see [[RDDPolygonalSummary]]
     */
    def polygonalSummaryValue[R](geometry: Geometry,
                                 visitor: GridVisitor[Raster[T], R],
                                 options: Rasterizer.Options)(
        implicit semigroupR: Semigroup[R], tagR: ClassTag[R]): PolygonalSummaryResult[R] = {
      self
        .polygonalSummary(self.sparkContext.parallelize(List(geometry)), visitor, options)
        .map { _.data }
        .reduce { _.combine(_) }
    }

    /**
     * Helper method for performing a polygonal summary across a raster RDD
     * that takes only a single Geometry and returns a single PolygonalSummaryResult[R]
     * for that Geometry. It uses the default Rasterizer.Options
     *
     * @see [[RDDPolygonalSummary]]
     */
    def polygonalSummaryValue[R](geometry: Geometry,
                                 visitor: GridVisitor[Raster[T], R])(
        implicit semigroupR: Semigroup[R], tagR: ClassTag[R]): PolygonalSummaryResult[R] = {
      self.polygonalSummaryValue(geometry, visitor, PolygonalSummary.DefaultOptions)
    }
  }

  trait ToRDDPolygonalSummaryMethods {
    implicit class withRDDPolygonalSummaryMethods[R, T <: CellGrid[Int]](
        val self: RDD[(SpatialKey, T)] with Metadata[TileLayerMetadata[SpatialKey]])
        extends RDDPolygonalSummaryMethods[R, T]
  }

  object ops extends ToRDDPolygonalSummaryMethods
}
