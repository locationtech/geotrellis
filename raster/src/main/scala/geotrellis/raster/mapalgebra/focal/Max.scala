package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                     the data values will be rounded to integers.
 */
object Max {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, bounds)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = Double.NaN
          cursor.allCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if (isData(v) && (v > m || isNoData(m))) {
              m = v
            }
          }
          resultTile.setDouble(cursor.col, cursor.row, m)
        }
      }

    } else {
      new CursorCalculation[Tile](tile, n, bounds)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = Int.MinValue
          cursor.allCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if(v > m) { m = v }
          }
          resultTile.set(cursor.col, cursor.row, m)
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}
