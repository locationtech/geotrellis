package geotrellis.raster.op.focal

import geotrellis.raster._

object MeanCalculation {

  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] with Initialization = {
    if(tile.cellType.isFloatingPoint) {
      n match {
        case Square(ext) => new CellwiseMeanCalcDouble
        case _ => new CursorMeanCalcDouble
      }
    } else {
      n match {
        case Square(ext) => new CellwiseMeanCalc
        case _ => new CursorMeanCalc
      }
    }
  }
}

case class CellwiseMeanCalc()
  extends CellwiseCalculation[Tile]
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

case class CursorMeanCalc()
  extends CursorCalculation[Tile]
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

case class CursorMeanCalcDouble()
  extends CursorCalculation[Tile]
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

case class CellwiseMeanCalcDouble()
  extends CellwiseCalculation[Tile]
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
