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

package geotrellis.spark.tiling

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._

import scala.reflect.ClassTag

class TilerMethods[K, V <: CellGrid[Int]: ClassTag: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])](val self: RDD[(K, V)]) extends MethodExtensions[RDD[(K, V)]] {
  import Tiler.Options

  def cutTiles[K2: SpatialComponent: ClassTag](cellType: CellType, layoutDefinition: LayoutDefinition, resampleMethod: ResampleMethod)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    CutTiles[K, K2, V](self, cellType, layoutDefinition, resampleMethod)

  def cutTiles[K2: SpatialComponent: ClassTag](cellType: CellType, layoutDefinition: LayoutDefinition)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    cutTiles(cellType, layoutDefinition, NearestNeighbor)

  def cutTiles[K2: SpatialComponent: ClassTag](tileLayerMetadata: TileLayerMetadata[K2], resampleMethod: ResampleMethod)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    cutTiles(tileLayerMetadata.cellType, tileLayerMetadata.layout, resampleMethod)

  def cutTiles[K2: SpatialComponent: ClassTag](tileLayerMetadata: TileLayerMetadata[K2])
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    cutTiles(tileLayerMetadata, NearestNeighbor)

  def tileToLayout[K2: SpatialComponent: ClassTag](cellType: CellType, layoutDefinition: LayoutDefinition, options: Options)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    CutTiles[K, K2, V](self, cellType, layoutDefinition, options.resampleMethod)
      .merge(options.partitioner)

  def tileToLayout[K2: SpatialComponent: ClassTag](cellType: CellType, layoutDefinition: LayoutDefinition)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] =
    tileToLayout(cellType, layoutDefinition, Options.DEFAULT)

  def tileToLayout[K2: SpatialComponent: ClassTag](tileLayerMetadata: TileLayerMetadata[K2], options: Options)
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] with Metadata[TileLayerMetadata[K2]] =
    ContextRDD(tileToLayout(tileLayerMetadata.cellType, tileLayerMetadata.layout, options), tileLayerMetadata)

  def tileToLayout[K2: SpatialComponent: ClassTag](tileLayerMetadata: TileLayerMetadata[K2])
      (implicit ev: K => TilerKeyMethods[K, K2]): RDD[(K2, V)] with Metadata[TileLayerMetadata[K2]] =
    tileToLayout(tileLayerMetadata, Options.DEFAULT)

}
