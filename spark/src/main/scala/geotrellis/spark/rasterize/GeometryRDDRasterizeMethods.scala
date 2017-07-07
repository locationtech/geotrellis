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

import geotrellis.raster._
import geotrellis.raster.rasterize._
//import geotrellis.raster.rasterize.Rasterizer
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.rasterize._
import geotrellis.util.MethodExtensions
import geotrellis.vector.Geometry
import org.apache.spark._
import org.apache.spark.rdd._

/**
  * Extension methods for invoking the rasterizer on RDD of Geometry objects.
  */
trait GeometryRDDRasterizeMethods[G <: Geometry] extends MethodExtensions[RDD[G]] {
  def rasterizeWithValue(
    value: Double,
    layout: LayoutDefinition
  )(
    cellType: CellType = IntConstantNoDataCellType,
    includePartial: Boolean = true,
    sampleType: PixelSampleType = PixelIsPoint,
    partitioner: Partitioner = new HashPartitioner(self.getNumPartitions)
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    RasterizeRDD.fromGeometry(self, value, layout, cellType,
      RasterizeRDD.Options(
        rasterizerOptions = Rasterizer.Options(includePartial, sampleType),
        partitioner = Some(partitioner)
      )
    )
  }
}
