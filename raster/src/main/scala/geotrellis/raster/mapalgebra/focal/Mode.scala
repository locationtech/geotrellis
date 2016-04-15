package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

/**
 * Computes the mode of a neighborhood for a given raster
 *
 * @note            Mode does not currently support Double raster data.
 *                  If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                  the data values will be rounded to integers.
 */
object Mode {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    n match {
      case Square(ext) => new CellwiseModeCalc(tile, n, target, bounds, ext)
      case _ => new CursorModeCalc(tile, n, target, bounds, n.extent)
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}


class CursorModeCalc(r: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds], extent: Int)
  extends CursorCalculation[Tile](r, n, target, bounds)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

  def calc(tile: Tile, cursor: Cursor) = {
    cursor.removedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      if(isData(v)) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x, y) =>
      val v = tile.get(x, y)
      if(isData(v)) addValue(v)
    }
    resultTile.set(cursor.col, cursor.row, mode)
  }
}


class CellwiseModeCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds], extent: Int)
  extends CellwiseCalculation[Tile](tile, n, target, bounds)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

  def add(tile: Tile, x: Int, y: Int) = {
    val v = tile.get(x, y)
    if (isData(v)) {
      addValue(v)
    }
  }

  def remove(tile: Tile, x: Int, y: Int) = {
    val v = tile.get(x, y)
    if (isData(v)) {
      removeValue(v)
    }
  }

  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidResult(x, y, focusCol, focusRow, mode)
}
