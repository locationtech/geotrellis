package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Computes the mode of a neighborhood for a given raster
 *
 * @note            Mode does not currently support Double raster data.
 *                  If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                  the data values will be rounded to integers.
 */
object Mode {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    n match {
      case Square(ext) => new CellwiseModeCalc(tile, n, bounds, ext)
      case _ => new CursorModeCalc(tile, n, bounds, n.extent)
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}


class CursorModeCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], extent: Int)
  extends CursorCalculation[Tile](r, n, bounds)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

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


class CellwiseModeCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], extent: Int)
  extends CellwiseCalculation[Tile](r, n, bounds)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

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