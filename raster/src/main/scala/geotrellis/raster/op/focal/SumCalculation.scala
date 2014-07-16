package geotrellis.raster.op.focal

import geotrellis.raster._

object SumCalculation {

  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] with Initialization = {
    if(tile.cellType.isFloatingPoint){
      n match {
        case Square(ext) => new CellwiseDoubleSumCalc
        case _ => new CursorDoubleSumCalc
      }
    }else{
      n match {
        case Square(ext) => new CellwiseSumCalc
        case _ => new CursorSumCalc
      }
    }

  }
}

class CursorSumCalc
  extends CursorCalculation[Tile]
  with IntArrayTileResult
{
  var total = 0

  def calc(r: Tile, cursor: Cursor) = {

    val added = collection.mutable.Set[(Int, Int, Int)]()
    cursor.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      added += ((x, y, v))
      if(isData(v)) { total += r.get(x, y) }
    }

    val removed = collection.mutable.Set[(Int, Int, Int)]()
    cursor.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      removed += ((x, y, v))
      if(isData(v)) { total -= r.get(x, y) }
    }

    tile.set(cursor.col, cursor.row, total)
  }
}

class CellwiseSumCalc
  extends CellwiseCalculation[Tile]
  with IntArrayTileResult
{
  var total = 0

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if(isData(v)) { total += r.get(x, y) }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if(isData(v)) { total -= r.get(x, y) }
  }

  def reset() = { total = 0}
  def setValue(x: Int, y: Int) = {
    tile.set(x, y, total)
  }
}

class CursorDoubleSumCalc
  extends CursorCalculation[Tile]
  with DoubleArrayTileResult
{
  var total = 0.0

  def calc(r: Tile, cursor: Cursor) = {

    val added = collection.mutable.Set[(Int, Int, Double)]()
    cursor.addedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      added += ((x, y, v))
      if(isData(v)) { total += r.getDouble(x, y) }
    }

    val removed = collection.mutable.Set[(Int, Int, Double)]()
    cursor.removedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      removed += ((x, y, v))
      if(isData(v)) { total -= r.getDouble(x, y) }
    }

    tile.setDouble(cursor.col, cursor.row, total)
  }
}

class CellwiseDoubleSumCalc
  extends CellwiseCalculation[Tile]
  with DoubleArrayTileResult
{
  var total = 0.0

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if(isData(v)) { total += r.getDouble(x, y) }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if(isData(v)) { total -= r.getDouble(x, y) }
  }

  def reset() = { total = 0.0 }
  def setValue(x: Int, y: Int) = {
    tile.setDouble(x, y, total)
  }
}
