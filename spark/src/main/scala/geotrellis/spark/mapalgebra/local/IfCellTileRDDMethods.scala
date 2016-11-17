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
import geotrellis.raster.mapalgebra.local.IfCell
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait IfCellTileRDDMethods[K] extends TileRDDMethods[K] {

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
    other: RDD[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int): RDD[(K, Tile)] = localIf(other, cond, trueValue, None)

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    partitioner: Option[Partitioner]
  ): RDD[(K, Tile)] = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double): RDD[(K, Tile)] = localIf(other, cond, trueValue, None)

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    partitioner: Option[Partitioner]
  ): RDD[(K, Tile)] = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue)
  }

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int): RDD[(K, Tile)] = localIf(other, cond, trueValue, falseValue, None)

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Int, Int) => Boolean,
    trueValue: Int,
    falseValue: Int,
    partitioner: Option[Partitioner]
  ): RDD[(K, Tile)] = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double): RDD[(K, Tile)] = localIf(other, cond, trueValue, falseValue, None)

  def localIf(
    other: RDD[(K, Tile)],
    cond: (Double, Double) => Boolean,
    trueValue: Double,
    falseValue: Double,
    partitioner: Option[Partitioner]
  ): RDD[(K, Tile)] = self.combineValues(other, partitioner) {
    case (r1, r2) => IfCell(r1, r2, cond, trueValue, falseValue)
  }
}
