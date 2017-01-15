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

package geotrellis.spark.streaming.buffer

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.spark._
import geotrellis.spark.buffer.{BufferSizes, BufferTiles, BufferedTile}
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

class BufferTilesMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
](val self: DStream[(K, V)]) extends MethodExtensions[DStream[(K, V)]] {
  def bufferTiles(bufferSize: Int): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, bufferSize))

  def bufferTiles(bufferSize: Int, layerBounds: GridBounds): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, bufferSize, layerBounds))

  def bufferTiles(bufferSizesPerKey: DStream[(K, BufferSizes)]): DStream[(K, BufferedTile[V])] =
    self.transformWith(bufferSizesPerKey, (selfRdd: RDD[(K, V)], bufferSizesPerKeyRdd: RDD[(K, BufferSizes)]) => BufferTiles(selfRdd, bufferSizesPerKeyRdd))

  def bufferTiles(getBufferSizes: K => BufferSizes): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, getBufferSizes))
}

