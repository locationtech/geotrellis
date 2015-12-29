/*
* Copyright (c) 2014 Azavea.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package geotrellis.raster.op.hydrology

import geotrellis.raster._
import geotrellis.raster.op.focal._

case class FillOptions(threshold: Double)
object FillOptions {
  val default = FillOptions(20.0)
}

/**
 * Fills sink values in a raster. Returns a raster of TypeDouble
 *
 * @note               The threshold in the options will be used to determine whether
 *                     or not something is a sink, so if you find that this operation
 *                     is incorrectly classifying your data, make sure you have set
 *                     the threshold appropriately.
 */
object Fill {
  def apply(r: Tile, n: Neighborhood, bounds: Option[GridBounds], threshold: Double): Tile = {
    if (r.cellType.isFloatingPoint) {
      new CursorFillCalcDouble(r, Square(1), bounds, threshold)
    } else {
      new CursorFillCalc(r, Square(1), bounds, threshold.toInt)
    }
  }.execute()
}

class CursorFillCalcDouble(r: Tile, n: Neighborhood, bounds: Option[GridBounds], threshold: Double)
  extends CursorCalculation[Tile](r, n, bounds)
  with DoubleArrayTileResult
{
  def calc(r: Tile, c: Cursor) = {
    var count: Int = 0
    var totalCount: Int = 0
    var sum: Double = 0
    val cVal: Double = r.getDouble(c.col, c.row)
    c.allCells.foreach { (col, row) =>
      if(c.col != col || c.row != row){
        if((r.getDouble(col, row) - cVal).abs > threshold ){
          count = count + 1
        }
        totalCount = totalCount + 1
        sum = sum + r.get(col, row)
      }
    }
    if(count == totalCount){
      resultTile.setDouble(c.col, c.row, sum / totalCount)
    } else {
      resultTile.setDouble(c.col, c.row, cVal)
    }
  }
}


class CursorFillCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], threshold: Int)
  extends CursorCalculation[Tile](r, n, bounds)
  with IntArrayTileResult
{
  def calc(r: Tile, c: Cursor) = {
    var count: Int = 0
    var totalCount: Int = 0
    var sum: Int = 0
    val cVal = r.get(c.col, c.row)
    c.allCells
      .foreach { (col, row) =>
      if (c.col != col || c.row != row) {
        if ((r.get(col, row) - cVal).abs > threshold) {
          count = count + 1
        }
        totalCount = totalCount + 1
        sum = sum + r.get(col, row)
      }
    }

    if(count == totalCount){
      resultTile.set(c.col, c.row, sum / totalCount)
    } else {
      resultTile.set(c.col, c.row, cVal)
    }
  }
}
