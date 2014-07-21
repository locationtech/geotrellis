package geotrellis.raster.op.focal

import geotrellis.raster._

object Sum {

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint){
      n match {
        case Square(ext) => new CellwiseDoubleSumCalc(tile, n, bounds)
        case _ =>           new CursorDoubleSumCalc(tile, n, bounds)
      }
    }else{
      n match {
        case Square(ext) => new CellwiseSumCalc(tile, n, bounds)
        case _ =>           new CursorSumCalc(tile, n, bounds)
      }
    }

  }
}

class CursorSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, bounds)
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

class CellwiseSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](r, n, bounds)
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

class CursorDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CursorCalculation[Tile](r, n, bounds)
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

class CellwiseDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
  extends CellwiseCalculation[Tile](r, n, bounds)
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
