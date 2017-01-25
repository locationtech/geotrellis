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
import geotrellis.raster.mapalgebra.local.Minority
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.util.MethodExtensions

trait MinorityTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: Seq[(K, Tile)]*)(implicit d: DI): Seq[(K, Tile)] =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: Seq[(K, Tile)]*)(implicit d: DI): Seq[(K, Tile)] =
    localMinority(n, rs)
}
