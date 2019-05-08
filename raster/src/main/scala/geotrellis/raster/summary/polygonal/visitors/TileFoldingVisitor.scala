/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.summary.polygonal.visitors

import geotrellis.raster._
import geotrellis.raster.summary.GridVisitor

/**
  * A Visitor that allows for user-defined aggregations over a Raster[Tile]
  *
  * This Visitor is used to implement a few aggregate operations in this package:
  * - [[MaxVisitor]]
  * - [[MinVisitor]]
  * - [[SumVisitor]]
  * - [[MeanVisitor]]
  *
  * This Visitor skips NaN values in the Raster, and will only call fold
  * when a new valid data value is encountered. You will need to implement
  * your own visitor if other behavior is desired.
  */
protected abstract class TileFoldingVisitor
    extends GridVisitor[Raster[Tile], Option[Double]] {
  private var accumulator: Double = Double.NaN

  def result: Option[Double] = if (isData(accumulator)) Some(accumulator) else None

  def visit(raster: Raster[Tile], col: Int, row: Int): Unit = {
    val newValue = raster.tile.getDouble(col, row)
    if (isData(newValue)) {
      if (isData(accumulator)) {
        accumulator = fold(accumulator, newValue)
      } else {
        accumulator = newValue
      }
    }
  }

  def fold(accum: Double, newValue: Double): Double
}
