package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

object Mean {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      n match {
        case Square(ext) => new CellwiseDoubleMeanCalc(tile, n, target, bounds)
        case _ => new CursorDoubleMeanCalc(tile, n, target, bounds)
      }
    } else {
      n match {
        case Square(ext) => new CellwiseMeanCalc(tile, n, target, bounds)
        case _ => new CursorMeanCalc(tile, n, target, bounds)
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}

class CellwiseMeanCalc(r: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
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

  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidDoubleResultFromInt(x, y, focusCol, focusRow, sum / count.toDouble)

  def reset() = { count = 0 ; sum = 0 }
}

class CursorMeanCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](tile, n, target, bounds)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Int = 0

  def calc(tile: Tile, cursor: Cursor) = {
    cursor.removedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      if(isData(v)) { count -= 1; sum -= v }
    }
    cursor.addedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      if(isData(v)) { count += 1; sum += v }
    }

    setValidDoubleResultFromInt(cursor, sum / count.toDouble)
  }
}

class CursorDoubleMeanCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](tile, n, target, bounds)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Double = 0.0

  def calc(tile: Tile, cursor: Cursor) = {
    cursor.removedCells.foreach { (x, y) =>
      val v = tile.getDouble(x, y)
      if(isData(v)) {
        count -= 1
        if (count == 0) sum = 0 else sum -= v
      }
    }
    cursor.addedCells.foreach { (x, y) =>
      val v = tile.getDouble(x, y)
      if(isData(v)) { count += 1; sum += v }
    }

    setValidDoubleResult(cursor, sum / count)
  }
}

class CellwiseDoubleMeanCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
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
  
  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidDoubleResult(x, y, focusCol, focusRow, sum / count.toDouble)

  def reset() = { count = 0 ; sum = 0.0 }
}
