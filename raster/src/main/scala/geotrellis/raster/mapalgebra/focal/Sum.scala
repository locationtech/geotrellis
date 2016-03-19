package geotrellis.raster.mapalgebra.focal

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
    extends CursorCalculation[Tile](r, n, bounds) with ArrayTileResult {

  var total: Int = NODATA

  def calc(r: Tile, cursor: Cursor) = {

    cursor.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if (isData(v)) {
        if (isData(total)) { total += v }
        else { total = v }
      }
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      // we do not remove from NODATA total, it would violate the cursor invarant
      if (isData(v) && isData(total)) total -= v
    }

    resultTile.set(cursor.col, cursor.row, total)
  }
}

class CellwiseSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](r, n, bounds) with ArrayTileResult {

  var total: Int = NODATA

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) {
      if (isData(total)) { total += v }
      else { total = v }
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    // we do not remove from NODATA total, it would violate the cursor invarant
    if (isData(v) && isData(total)) total -= v
  }

  def reset() = total = NODATA

  def setValue(x: Int, y: Int) = resultTile.set(x, y, total)
}

class CursorDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CursorCalculation[Tile](r, n, bounds) with ArrayTileResult {

  // keep track of count so we know when to reset total to minimize floating point errors
  var count: Int = 0
  var total: Double = Double.NaN

  def calc(r: Tile, cursor: Cursor) = {
    cursor.addedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if (isData(v)) {
        if (isData(total)) {total += v; count += 1}
        else { total = v; count = 1 }
      }
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = r.getDouble(x, y)
      if (isData(v)) {
        count -= 1
        if (count == 0) total = Double.NaN else total -= v
      }
    }

    resultTile.setDouble(cursor.col, cursor.row, total)
  }
}

class CellwiseDoubleSumCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](r, n, bounds) with ArrayTileResult {

  // keep track of count so we know when to reset total to minimize floating point errors
  var count: Int = 0
  var total: Double = Double.NaN

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if (isData(v)) {
      if (isData(total)) { total += v; count += 1 }
      else { total = v; count = 1 }
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.getDouble(x, y)
    if (isData(v)) {
      count -= 1
      if (count == 0) total = Double.NaN else total -= v
    }
  }

  def reset() = total = Double.NaN

  def setValue(x: Int, y: Int) = resultTile.setDouble(x, y, total)
}
