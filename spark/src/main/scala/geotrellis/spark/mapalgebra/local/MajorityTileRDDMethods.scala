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

import org.apache.spark.Partitioner

import scala.collection.immutable.Map

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.Majority

import geotrellis.spark._
import geotrellis.spark.mapalgebra._

import org.apache.spark.rdd.RDD

trait MajorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMajority(others, None)
  def localMajority(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Majority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: RDD[(K, Tile)]*): RDD[(K, Tile)] =
    localMajority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMajority(n, others, None)
  def localMajority(n: Int, others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner) { tiles => Majority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: RDD[(K, Tile)]*): RDD[(K, Tile)] =
    localMajority(n, rs)
}
