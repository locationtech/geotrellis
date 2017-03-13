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
import geotrellis.raster.mapalgebra.local.Or
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait OrTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int) =
    self.mapValues { r => Or(r, i) }

  /** Or a constant Int value to each cell. */
  def |(i: Int) = localOr(i)

  /** Or a constant Int value to each cell. */
  def |:(i: Int) = localOr(i)

  /** Or the values of each cell in each raster.  */
  def localOr(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localOr(other, None)
  def localOr(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(r: RDD[(K, Tile)]): RDD[(K, Tile)] = localOr(r, None)

  /** Or the values of each cell in each raster.  */
  def localOr(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localOr(others, None)
  def localOr(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localOr(others, None)
}
