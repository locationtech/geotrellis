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

package geotrellis.spark.buffer

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.buffer.{BufferSizes, BufferedTile}
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.layers.buffer.BufferTiles
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner

import scala.reflect.ClassTag


class BufferTilesRDDMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
](val self: RDD[(K, V)]) extends MethodExtensions[RDD[(K, V)]] {
  def bufferTiles(bufferSize: Int): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, bufferSize)

  def bufferTiles(bufferSize: Int, partitioner: Option[Partitioner]): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, bufferSize, partitioner)

  def bufferTiles(bufferSize: Int, layerBounds: TileBounds): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, bufferSize, layerBounds)

  def bufferTiles(
    bufferSize: Int,
    layerBounds: TileBounds,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, bufferSize, layerBounds, partitioner)

  def bufferTiles(getBufferSizes: K => BufferSizes): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, getBufferSizes)

  def bufferTiles(
    getBufferSizes: K => BufferSizes,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] =
    BufferTilesRDD(self, getBufferSizes, partitioner)
}
