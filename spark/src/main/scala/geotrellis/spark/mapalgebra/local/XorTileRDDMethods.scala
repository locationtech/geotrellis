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

package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Xor
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait XorTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) =
    self.mapValues { r => Xor(r, i) }

  /** Xor a constant Int value to each cell. */
  def ^(i: Int) = localXor(i)

  /** Xor a constant Int value to each cell. */
  def ^:(i: Int) = localXor(i)

  /** Xor the values of each cell in each raster.  */
  def localXor(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localXor(other, None)
  def localXor(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: TileLayerRDD[K]): RDD[(K, Tile)] = localXor(r, None)
  
  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localXor(others, None)
  def localXor(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localXor(others, None)
}
