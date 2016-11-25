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
import geotrellis.raster.mapalgebra.local.Min
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait MinTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) =
    self.mapValues { r => Min(r, i) }

  /** Min a constant Double value to each cell. */
  def localMin(d: Double) =
    self.mapValues { r => Min(r, d) }

  /** Min the values of each cell in each raster.  */
  def localMin(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localMin(other, None)
  def localMin(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Min.apply)

  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMin(others, None)
  def localMin(others: Seq[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Min.apply)
}
