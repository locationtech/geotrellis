package geotrellis.raster.op.focal

import geotrellis.raster._
import spire.syntax.cfor._

/**
 * Computes the convolution of a raster with a kernel.
 *
 * @param      tile         Tile to convolve.
 * @param      kernel       Kernel that represents the convolution filter.
 * @param      bounds       Optionla bounds of the analysis area that we are convolving.
 */
object Convolve {
  def calculation(tile: Tile, kernel: Kernel, bounds: Option[GridBounds] = None): KernelCalculation[Tile] = {
    val resultTile = ArrayTile.empty(tile.cellType, tile.cols, tile.rows)

    if (tile.cellType.isFloatingPoint) {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel, bounds) {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = Double.NaN
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = t.getDouble(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            resultTile.setDouble(cursor.col, cursor.row, s)
          }

          def result = resultTile
        }
      } else {
        new KernelCalculation[Tile](tile, kernel, bounds) {
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

          def result = resultTile
        }
      }
    } else {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel, bounds) {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = t.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w).toInt }
                else { s = (v * w).toInt }
              }
            }
            resultTile.set(cursor.col, cursor.row, s)
          }

          def result = resultTile
        }
      } else {
        new KernelCalculation[Tile](tile, kernel, bounds) {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeight { (x, y, w) =>
              val v = t.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            resultTile.set(cursor.col, cursor.row, s)
          }

          def result = resultTile
        }
      }
    }
  }

  def apply(tile: Tile, kernel: Kernel, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, kernel, bounds).execute()
}
