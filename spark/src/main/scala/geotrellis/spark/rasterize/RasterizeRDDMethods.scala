/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.rasterize

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.rasterize._
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}
import org.apache.spark._
import org.apache.spark.rdd._

/**
  * Extension methods for invoking the rasterizer on RDD of Geometry objects.
  */
trait GeometryRDDRasterizeMethods[G <: Geometry] extends MethodExtensions[RDD[G]] {

  /**
   * Rasterize an RDD of Geometry objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Value will be converted to type matching specified [[CellType]].
   *
   * @param value Cell value for cells intersecting a geometry
   * @param cellType [[CellType]] for creating raster tiles
   * @param layout Raster layer layout for the result of rasterization
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def rasterize(
    value: Double,
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    RasterizeRDD.fromGeometry(self, value, cellType, layout, options, partitioner)
  }
}

/**
 * Extension methods for invoking the rasterizer on RDD of Feature objects.
 */
trait FeatureRDDRasterizeMethods[G <: Geometry] extends MethodExtensions[RDD[Feature[G, Double]]] {

  /**
   * Rasterize an RDD of Feature objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Feature data will be converted to type matching specified [[CellType]].
   * Feature rasterization order is undefined in this operation.
   *
   * @param cellType [[CellType]] for creating raster tiles
   * @param layout Raster layer layout for the result of rasterization
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def rasterize(
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    RasterizeRDD.fromFeature(self, cellType, layout, options, partitioner)
  }
}

/**
  * Extension methods for invoking the rasterizer on RDD of Feature objects with CellValue data.
  */
trait CellValueFeatureRDDRasterizeMethods[G <: Geometry] extends MethodExtensions[RDD[Feature[G, CellValue]]] {

  /**
   * Rasterize an RDD of Feature objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Feature value will be converted to type matching specified [[CellType]].
   * Z-Index from [[CellValue]] will be maintained per-cell during rasterization.
   * A cell with greater zindex is always in front of a cell with a lower zinde.
   *
   * @param cellType [[CellType]] for creating raster tiles
   * @param layout Raster layer layout for the result of rasterization
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def rasterize(
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    RasterizeRDD.fromFeatureWithZIndex(self, cellType, layout, options, partitioner)
  }
}
