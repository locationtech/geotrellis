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

import spire.syntax.cfor._

/**
  * A Visitor that allows for user-defined aggregations over a Raster[MultibandTile]
  *
  * This Visitor has the same semantics as [[TileFoldingVisitor]], its just the
  * MultibandTile version.
  *
  * @todo test performance and refactor further if necessary
  */
protected abstract class MultibandTileFoldingVisitor
    extends GridVisitor[Raster[MultibandTile], Array[Option[Double]]] {

  private var initialized = false
  private var accumulator = Array[Double]()

  def result: Array[Option[Double]] = {
    accumulator.map { value =>
      if (isData(value)) Some(value) else None
    }
  }

  def visit(raster: Raster[MultibandTile], col: Int, row: Int): Unit = {
    val bandCount = raster.tile.bandCount
    if (!initialized) {
      accumulator = Array.fill[Double](bandCount)(Double.NaN)
      initialized = true
    }
    cfor(0)(_ < bandCount, _ + 1) { i =>
      val newValue = raster.tile.band(i).getDouble(col, row)
      val accum = accumulator(i)
      if (isData(newValue)) {
        accumulator(i) = if (isData(accum)) {
          fold(accum, newValue)
        } else {
          newValue
        }
      }
    }
  }

  def fold(accum: Double, newValue: Double): Double
}
