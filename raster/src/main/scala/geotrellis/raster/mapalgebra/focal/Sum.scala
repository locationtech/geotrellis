package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

object Sum {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) n match {
      case Square(ext) => new CellwiseDoubleSumCalc(tile, n, target, bounds)
      case _ =>           new CursorDoubleSumCalc(tile, n, target, bounds)
    } else n match {
      case Square(ext) => new CellwiseSumCalc(tile, n, target, bounds)
      case _ =>           new CursorSumCalc(tile, n, target, bounds)
    }

  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute
}

class CursorSumCalc(r: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
    extends CursorCalculation[Tile](r, n, target, bounds) with ArrayTileResult {

  var total: Int = NODATA

  def calc(tile: Tile, cursor: Cursor) = {

    cursor.addedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      if (isData(v)) {
        if (isData(total)) {
          total += v
        }
        else {
          total = v
        }
      }
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      // we do not remove from NODATA total, it would violate the cursor invarant
      if (isData(v) && isData(total)) total -= v
    }

    setValidResult(cursor, total)
  }
}

class CellwiseSumCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](tile, n, target, bounds) with ArrayTileResult {

  var total: Int = NODATA

  def add(tile: Tile, x: Int, y: Int) = {
    val v = tile.get(x, y)
    //println(s"Add Evaluation ($x, $y)")
    if (isData(v)) {
      if (isData(total)) { total += v }
      else { total = v }
    }
  }

  def remove(tile: Tile, x: Int, y: Int) = {
    val v = tile.get(x, y)
    //println(s"Remove Evaluation ($x, $y)")
    // we do not remove from NODATA total, it would violate the cursor invarant
    if (isData(v) && isData(total)) total -= v
  }

  def reset() =
    total = NODATA

  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidResult(x, y, focusCol, focusRow, total)
}

class CursorDoubleSumCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
    extends CursorCalculation[Tile](tile, n, target, bounds) with ArrayTileResult {

  // keep track of count so we know when to reset total to minimize floating point errors
  var count: Int = 0
  var total: Double = doubleNODATA

  def calc(tile: Tile, cursor: Cursor) = {
    cursor.addedCells.foreach { (x, y) =>
      val v = tile.getDouble(x, y)
      if (isData(v)) {
        if (isData(total)) {total += v; count += 1}
        else { total = v; count = 1 }
      }
    }

    cursor.removedCells.foreach { (x, y) =>
      val v = tile.getDouble(x, y)
      if (isData(v)) {
        count -= 1
        if (count == 0) total = doubleNODATA else total -= v
      }
    }

    setValidDoubleResult(cursor, total)
  }
}

class CellwiseDoubleSumCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds])
    extends CellwiseCalculation[Tile](tile, n, target, bounds) with ArrayTileResult {

  // keep track of count so we know when to reset total to minimize floating point errors
  var count: Int = 0
  var total: Double = doubleNODATA

  def add(tile: Tile, x: Int, y: Int) = {
    val v = tile.getDouble(x, y)
    if (isData(v)) {
      if (isData(total)) { total += v; count += 1 }
      else { total = v; count = 1 }
    }
  }

  def remove(tile: Tile, x: Int, y: Int) = {
    val v = tile.getDouble(x, y)
    if (isData(v)) {
      count -= 1
      if (count == 0) total = doubleNODATA else total -= v
    }
  }

  def reset() =
    total = doubleNODATA

  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidDoubleResult(x, y, focusCol, focusRow, total)
}
