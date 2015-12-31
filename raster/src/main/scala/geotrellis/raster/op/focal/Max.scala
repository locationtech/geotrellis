package geotrellis.raster.op.focal

import geotrellis.raster.{GridBounds, Tile}

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Max {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, bounds)
        with DoubleArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = Double.MinValue
          cursor.allCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(v > m) { m = v }
          }
          resultTile.setDouble(cursor.col, cursor.row, m)
        }
      }

    } else {
      new CursorCalculation[Tile](tile, n, bounds)
        with IntArrayTileResult
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

