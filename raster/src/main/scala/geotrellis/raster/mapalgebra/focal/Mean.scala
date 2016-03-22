package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.FocalTarget.FocalTarget

object Mean {
  def calculation(tile: Tile, n: Neighborhood, target: FocalTarget = FocalTarget.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      n match {
        case Square(ext) => new CellwiseMeanCalcDouble(tile, n, target, bounds)
        case _ => new CursorMeanCalcDouble(tile, n, target, bounds)
      }
    } else {
      n match {
        case Square(ext) => new CellwiseMeanCalc(tile, n, target, bounds)
        case _ => new CursorMeanCalc(tile, n, target, bounds)
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: FocalTarget = FocalTarget.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}

class CellwiseMeanCalc(r: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](r, n, target, bounds)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Int = 0

  def add(tile: Tile, x: Int, y: Int) = {
    val z = tile.get(x, y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(tile: Tile, x: Int, y: Int) = {
    val z = tile.get(x, y)
    if (isData(z)) {
      count -= 1
      sum -= z
    }
  }

  def setValue(x: Int, y: Int) = { resultTile.setDouble(x, y, sum / count.toDouble) }

  def reset() = { count = 0 ; sum = 0 }

  def copy(focusCol: Int, focusRow: Int, x: Int, y: Int) =
    resultTile.set(x, y, tile.get(focusCol, focusRow))
}

class CursorMeanCalc(r: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, target, bounds)
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

class CursorMeanCalcDouble(r: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, target, bounds)
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

class CellwiseMeanCalcDouble(tile: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](tile, n, target, bounds)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Double = 0.0

  def add(tile: Tile, x: Int, y: Int) = {
    val z = tile.getDouble(x, y)
    if (isData(z)) {
      count += 1
      sum   += z
    }
  }

  def remove(tile: Tile, x: Int, y: Int) = {
    val v = tile.getDouble(x, y)
    if(isData(v)) {
      count -= 1
      if (count == 0) sum = 0 else sum -= v
    }
  }

  def setValue(x: Int, y: Int) = { resultTile.setDouble(x, y, sum / count) }

  def reset() = { count = 0 ; sum = 0.0 }

  def copy(focusCol: Int, focusRow: Int, x: Int, y: Int) =
    resultTile.setDouble(x, y, tile.getDouble(focusCol, focusRow))
}
