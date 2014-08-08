package geotrellis.raster.op.focal

import geotrellis.raster._

object Mean {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      n match {
        case Square(ext) => new CellwiseMeanCalcDouble(tile, n, bounds)
        case _ => new CursorMeanCalcDouble(tile, n, bounds)
      }
    } else {
      n match {
        case Square(ext) => new CellwiseMeanCalc(tile, n, bounds)
        case _ => new CursorMeanCalc(tile, n, bounds)
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}

class CellwiseMeanCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](r, n, bounds)
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

  def setValue(x: Int, y: Int) = { tile.setDouble(x, y, sum / count.toDouble) }
  def reset() = { count = 0 ; sum = 0 }
}

class CursorMeanCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, bounds)
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
    tile.setDouble(c.col, c.row, sum / count.toDouble)
  }
}

class CursorMeanCalcDouble(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, bounds)
  with DoubleArrayTileResult
{
  var count: Int = 0
  var sum: Double = 0.0

  def calc(r: Tile, c: Cursor) = {
    c.removedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if(isData(v)) { count -= 1; sum -= v }
    }
    c.addedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if(isData(v)) { count += 1; sum += v }
    }
    tile.setDouble(c.col, c.row, sum / count)
  }
}

class CellwiseMeanCalcDouble(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](r, n, bounds)
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
    val z = r.getDouble(x, y)
    if (isData(z)) {
      count -= 1
      sum -= z
    }
  }

  def setValue(x: Int, y: Int) = { tile.setDouble(x, y, sum / count) }
  def reset() = { count = 0 ; sum = 0.0 }
}
