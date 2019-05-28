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

package geotrellis.spark.crop

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.crop.TileCropMethods
import geotrellis.raster.crop.Crop.Options
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector.Extent
import org.apache.spark.rdd.RDD

abstract class LayerRDDCropMethods[
    K: SpatialComponent,
    V <: CellGrid[Int]: (? => TileCropMethods[V]),
    M: Component[?, Bounds[K]]: GetComponent[?, Extent]: GetComponent[?, LayoutDefinition]
  ] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def crop(extent: Extent, options: Options): RDD[(K, V)] with Metadata[M] =
    Crop(self, extent, options)

  def crop(extent:Extent): RDD[(K, V)] with Metadata[M] =
    crop(extent, Options.DEFAULT)
}
