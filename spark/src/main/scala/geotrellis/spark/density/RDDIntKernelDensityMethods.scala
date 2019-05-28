/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.density

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Kernel
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import geotrellis.vector._
import org.apache.spark.rdd.RDD

trait RDDIntKernelDensityMethods extends MethodExtensions[RDD[PointFeature[Int]]] {
  def kernelDensity(kernel: Kernel, layoutDefinition: LayoutDefinition, crs: CRS): RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
    RDDKernelDensity(self, layoutDefinition, kernel, crs)

  def kernelDensity(kernel: Kernel, layoutDefinition: LayoutDefinition, crs: CRS, cellType: CellType): RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
    RDDKernelDensity(self, layoutDefinition, kernel, crs, cellType)
}
