package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

/**
 * Computes the minimum value of a neighborhood for a given raster
 */
object Min {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {

    if (tile.cellType.isFloatingPoint)
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m: Double = Double.NaN
          cursor.allCells.foreach { (col, row) =>
            val v = r.getDouble(col, row)
            if (isData(v) && (v < m || isNoData(m))) {
              m = v
            }
          }
          resultTile.setDouble(cursor.col, cursor.row, m)
        }
      }

    else
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = NODATA
          cursor.allCells.foreach { (col, row) =>
            val v = r.get(col, row)
            if(isData(v) && (v < m || isNoData(m))) { m = v }
          }

          resultTile.set(cursor.col, cursor.row, m)
        }
      }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}
