package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

/**
 * Computes the minimum value of a neighborhood for a given raster
 *
 * @note            Min does not currently support Double raster data.
 *                  If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                  the data values will be rounded to integers.
 */
object Min {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {

    if (tile.cellType.isFloatingPoint)
      new CursorCalculation[Tile](tile, n, target, bounds)
        with ArrayTileResult
      {
        def calc(tile: Tile, cursor: Cursor) = {
          var min: Double = doubleNODATA
          cursor.allCells.foreach { (col, row) =>
            val v = tile.getDouble(col, row)
            if (isData(v) && (v < min || isNoData(min))) {
              min = v
            }
          }
          setValidDoubleResult(cursor, min)
        }
      }

    else
      new CursorCalculation[Tile](tile, n, target, bounds)
        with ArrayTileResult
      {
        def calc(tile: Tile, cursor: Cursor) = {
          var min = NODATA
          cursor.allCells.foreach { (col, row) =>
            val v = tile.get(col, row)
            if(isData(v) && (v < min || isNoData(min))) { min = v }
          }

          setValidResult(cursor, min)
        }
      }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}
