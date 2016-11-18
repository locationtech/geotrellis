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
import geotrellis.raster.mapalgebra.local.And
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait AndTileRDDMethods[K] extends TileRDDMethods[K] {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) =
    self.mapValues { r => And(r, i) }

  /** And a constant Int value to each cell. */
  def &(i: Int) = localAnd(i)

  /** And a constant Int value to each cell. */
  def &:(i: Int) = localAnd(i)

  /** And the values of each cell in each raster.  */
  def localAnd(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localAnd(other, None)
  def localAnd(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(rs: TileLayerRDD[K]): RDD[(K, Tile)] = localAnd(rs, None)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAnd(others, None)
  def localAnd(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAnd(others, None)
}
