package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Computes the mode of a neighborhood for a given raster
 *
 * @note            Mode does not currently support Double raster data.
 *                  If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                  the data values will be rounded to integers.
 */
object ModeCalculation {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] =
    n match {
      case Square(ext) => new CellwiseModeCalc(tile, n, ext)
      case _ => new CursorModeCalc(tile, n, n.extent)
    }
}


class CursorModeCalc(r: Tile, n: Neighborhood, extent: Int)
  extends CursorCalculation[Tile](r, n)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)
  init(r)

  def calc(r: Tile, cursor: Cursor) = {
    cursor.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) addValue(v)
    }
    tile.set(cursor.col, cursor.row, mode)
  }
}


class CellwiseModeCalc(r: Tile, n: Neighborhood, extent: Int)
  extends CellwiseCalculation[Tile](r, n)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)
  init(r)

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) {
      addValue(v)
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) {
      removeValue(v)
    }
  }

  def setValue(x: Int, y: Int) = {
    tile.setDouble(x, y, mode)
  }
}