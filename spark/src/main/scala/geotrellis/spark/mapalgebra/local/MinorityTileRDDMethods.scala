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
import geotrellis.raster.mapalgebra.local.Minority
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait MinorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMinority(others, None)
  def localMinority(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: RDD[(K, Tile)]*)(implicit d: DI): RDD[(K, Tile)] =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMinority(n, others, None)
  def localMinority(n: Int, others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: RDD[(K, Tile)]*)(implicit d: DI): RDD[(K, Tile)] =
    localMinority(n, rs)
}
