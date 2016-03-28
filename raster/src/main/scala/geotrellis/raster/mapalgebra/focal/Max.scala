package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                     the data values will be rounded to integers.
 */
object Max {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, target, bounds)
        with ArrayTileResult
      {
        def calc(tile: Tile, cursor: Cursor) = {
          var max = doubleNODATA
          cursor.allCells.foreach { (x, y) =>
            val v = tile.getDouble(x, y)
            if (isData(v) && (v > max || isNoData(max))) {
              max = v
            }
          }

          setValidDoubleResult(cursor, max)
        }
      }

    } else {
      new CursorCalculation[Tile](tile, n, target, bounds)
        with ArrayTileResult
      {
        def calc(tile: Tile, cursor: Cursor) = {
          var max = Int.MinValue
          cursor.allCells.foreach { (x, y) =>
            val v = tile.get(x, y)
            if(v > max) { max = v }
          }
          setValidResult(cursor, max)
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}
