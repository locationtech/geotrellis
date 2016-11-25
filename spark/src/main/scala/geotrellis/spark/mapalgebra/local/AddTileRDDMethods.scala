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

import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.Add
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait AddTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) =
    self.mapValues { r => Add(r, i) }

  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)

  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)

  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) =
    self.mapValues { r => Add(r, d) }

  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)

  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)

  /** Add the values of each cell in each raster.  */
  def localAdd(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localAdd(other, None)
  def localAdd(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner) { Add.apply }

  /** Add the values of each cell in each raster. */
  def +(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localAdd(other, None)

  def localAdd(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAdd(others, None)
  def localAdd(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner) { Add.apply }

  def +(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAdd(others, None)
}
