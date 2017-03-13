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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

object Mean {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      n match {
        case Square(ext) => new CellwiseMeanCalcDouble(tile, n, bounds, target)
        case _ => new CursorMeanCalcDouble(tile, n, bounds, target)
      }
    } else {
      n match {
        case Square(ext) => new CellwiseMeanCalc(tile, n, bounds, target)
        case _ => new CursorMeanCalc(tile, n, bounds, target)
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}

class CellwiseMeanCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell)
  extends CellwiseCalculation[Tile](r, n, bounds, target)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Int = 0

  def add(r: Tile, x: Int, y: Int) = {
    val z = r.get(x, y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val z = r.get(x, y)
    if (isData(z)) {
      count -= 1
      sum -= z
    }
  }

  def setValue(x: Int, y: Int) = { resultTile.setDouble(x, y, sum / count.toDouble) }
  def reset() = { count = 0 ; sum = 0 }
}

class CursorMeanCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell)
  extends CursorCalculation[Tile](r, n, bounds, target)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Int = 0

  def calc(r: Tile, c: Cursor) = {
    c.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) { count -= 1; sum -= v }
    }
    c.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) { count += 1; sum += v }
    }
    resultTile.setDouble(c.col, c.row, sum / count.toDouble)
  }
}

class CursorMeanCalcDouble(r: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell)
  extends CursorCalculation[Tile](r, n, bounds, target)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Double = 0.0

  def calc(r: Tile, c: Cursor) = {
    c.removedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if(isData(v)) {
        count -= 1
        if (count == 0) sum = 0 else sum -= v
      }
    }
    c.addedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if(isData(v)) { count += 1; sum += v }
    }
    resultTile.setDouble(c.col, c.row, sum / count)
  }
}

class CellwiseMeanCalcDouble(r: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell)
  extends CellwiseCalculation[Tile](r, n, bounds, target)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Double = 0.0

  def add(r: Tile, x: Int, y: Int) = {
    val z = r.getDouble(x, y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if(isData(v)) {
      count -= 1
      if (count == 0) sum = 0 else sum -= v
    }
  }

  def setValue(x: Int, y: Int) = { resultTile.setDouble(x, y, sum / count) }
  def reset() = { count = 0 ; sum = 0.0 }
}
