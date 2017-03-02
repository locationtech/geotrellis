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
import geotrellis.raster.mapalgebra.local.Multiply
import geotrellis.util.MethodExtensions

trait MultiplyTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) =
    self.mapValues { r => Multiply(r, i) }

  /** Multiply a constant value from each cell.*/
  def *(i: Int) = localMultiply(i)

  /** Multiply a constant value from each cell.*/
  def *:(i: Int) = localMultiply(i)

  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) =
    self.mapValues { r => Multiply(r, d) }

  /** Multiply a double constant value from each cell.*/
  def *(d: Double) = localMultiply(d)

  /** Multiply a double constant value from each cell.*/
  def *:(d: Double) = localMultiply(d)

  /** Multiply the values of each cell in each raster. */
  def localMultiply(other: Seq[(K, Tile)]): Seq[(K, Tile)] = {
    self.combineValues(other)(Multiply.apply)
  }

  /** Multiply the values of each cell in each raster. */
  def *(other: Seq[(K, Tile)]): Seq[(K, Tile)] = localMultiply(other)

  /** Multiply the values of each cell in each raster. */
  def localMultiply(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Multiply.apply)

  /** Multiply the values of each cell in each raster. */
  def *(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localMultiply(others)
}
