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
import geotrellis.raster.mapalgebra.local.Min
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.util.MethodExtensions

trait MinTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) =
    self.mapValues { r => Min(r, i) }

  /** Min a constant Double value to each cell. */
  def localMin(d: Double) =
    self.mapValues { r => Min(r, d) }

  /** Min the values of each cell in each raster.  */
  def localMin(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Min.apply)

  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[Seq[(K, Tile)]])(implicit d: DI): Seq[(K, Tile)] =
    self.combineValues(others)(Min.apply)
}
