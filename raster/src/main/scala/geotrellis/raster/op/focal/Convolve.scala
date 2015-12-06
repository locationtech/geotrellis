package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Convolve {
  def calculation(tile: Tile, n: Kernel, bounds: Option[GridBounds] = None): KernelCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      new KernelCalculation[Tile](tile, n, bounds)
          with DoubleArrayTileResult
      {
        def calc(t: Tile, cursor: KernelCursor) = {
          var s = Double.NaN
          cursor.foreachWithWeight { (x, y, w) =>
            val v = t.getDouble(x, y)
            if(isData(v)) {
              if(isData(s)) { s = s + (v * w) }
              else { s = v * w }
            }
          }
          resultTile.setDouble(cursor.col, cursor.row, s)
        }
      }
    } else {
      new KernelCalculation[Tile](tile, n, bounds)
          with DoubleArrayTileResult
      {

        def calc(t: Tile, cursor: KernelCursor) = {
          var s = NODATA
          cursor.foreachWithWeight { (x, y, w) =>
            val v = t.get(x, y)
            if(isData(v)) {
              if(isData(s)) { s = s + (v * w).toInt }
              else { s = (v * w).toInt }
            }
          }
          resultTile.set(cursor.col, cursor.row, s)
        }
      }
    }
  }

  def apply(tile: Tile, k: Kernel, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, k, bounds).execute()
}
