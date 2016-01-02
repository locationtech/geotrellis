package geotrellis.raster.op.focal

import geotrellis.raster._

object Sum {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) n match {
      case Square(ext) => new CellwiseDoubleSumCalc(tile, n, bounds)
      case _ =>           new CursorDoubleSumCalc(tile, n, bounds)
    } else n match {
      case Square(ext) => new CellwiseSumCalc(tile, n, bounds)
      case _ =>           new CursorSumCalc(tile, n, bounds)
    }

  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute
}

class CursorSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CursorCalculation[Tile](r, n, bounds) with IntArrayTileResult {

  var total = 0

  def calc(r: Tile, cursor: Cursor) = {

    cursor.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if (isData(v)) total += v
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if (isData(v)) total -= v
    }

    resultTile.set(cursor.col, cursor.row, total)
  }
}

class CellwiseSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](r, n, bounds) with IntArrayTileResult {

  var total = 0

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) total += v
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) total -= v
  }

  def reset() = total = 0

  def setValue(x: Int, y: Int) = resultTile.set(x, y, total)
}

class CursorDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CursorCalculation[Tile](r, n, bounds) with DoubleArrayTileResult {

  var total = 0.0

  def calc(r: Tile, cursor: Cursor) = {
    cursor.addedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if (isData(v)) total += v
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if (isData(v)) total -= v
    }

    resultTile.setDouble(cursor.col, cursor.row, total)
  }
}

class CellwiseDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](r, n, bounds) with DoubleArrayTileResult {

  var total = 0.0

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if (isData(v)) total += v
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if (isData(v)) total -= v
  }

  def reset() = total = 0.0

  def setValue(x: Int, y: Int) = resultTile.setDouble(x, y, total)
}
