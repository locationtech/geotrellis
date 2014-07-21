package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Computes the minimum value of a neighborhood for a given raster
 *
 * @note            Min does not currently support Double raster data.
 *                  If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                  the data values will be rounded to integers.
 */
object Min {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] = {

    if (tile.cellType.isFloatingPoint)
      new CursorCalculation[Tile](tile, n)
        with DoubleArrayTileResult
      {
        init(r)
        def calc(r: Tile, cursor: Cursor) = {
          var m: Double = Double.NaN
          cursor.allCells.foreach { (col, row) =>
            val v = r.getDouble(col, row)
            if (isData(v) && (v < m || isNoData(m))) {
              m = v
            }
          }
          tile.setDouble(cursor.col, cursor.row, m)
        }
      }

    else
      new CursorCalculation[Tile](tile, n)
        with IntArrayTileResult
      {
        init(r)
        def calc(r: Tile, cursor: Cursor) = {
          var m = NODATA
          cursor.allCells.foreach { (col, row) =>
              val v = r.get(col, row)
              if(isData(v) && (v < m || isNoData(m))) { m = v }
          }
          tile.set(cursor.col, cursor.row, m)
        }
      }
  }
}

