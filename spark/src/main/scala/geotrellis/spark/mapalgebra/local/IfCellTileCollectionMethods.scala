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
import geotrellis.raster.mapalgebra.local.IfCell
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.util.MethodExtensions

trait IfCellTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {

  def localIf(cond: Int => Boolean, trueValue: Int) =
    self.mapValues { r => IfCell(r, cond, trueValue) }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double
  ) = self.mapValues { r => IfCell(r, cond, trueValue) }

  def localIf(
    cond: Int => Boolean,
    trueValue: Int,
    falseValue: Int
  ) = self.mapValues {
    r => IfCell(r, cond, trueValue, falseValue)
  }

  def localIf(
    cond: Double => Boolean,
    trueValue: Double,
    falseValue: Double
  ) = self.mapValues {
    r => IfCell(r, cond, trueValue, falseValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }

  def localIf(
    other: Seq[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double
  ): Seq[(K, Tile)] = self.combineValues(other) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }
}
