package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell
import spire.syntax.cfor._

/**
 * Computes the convolution of a raster with a kernel.
 *
 * @param      tile         Tile to convolve.
 * @param      kernel       Kernel that represents the convolution filter.
 * @param      bounds       Optionla bounds of the analysis area that we are convolving.
 */
object Convolve {
  def calculation(tile: Tile, kernel: Kernel, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): KernelCalculation[Tile] = {
    val resultTile = ArrayTile.empty(tile.cellType, tile.cols, tile.rows)

    if (tile.cellType.isFloatingPoint) {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel,  target, bounds) {
          def calc(tile: Tile, cursor: KernelCursor) = {
            var s = doubleNODATA
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = tile.getDouble(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }

            setValidDoubleResult(cursor, s)
          }

          def result = resultTile

          def set(x: Int, y: Int, value: Int) =
            resultTile.set(x, y, value)

          def setDouble(x: Int, y: Int, value: Double) =
            resultTile.setDouble(x, y, value)
        }
      } else {
        new KernelCalculation[Tile](tile, kernel, target, bounds) {
          def calc(tile: Tile, cursor: KernelCursor) = {
            var s = doubleNODATA
            cursor.foreachWithWeight { (x, y, w) =>
              val v = tile.getDouble(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            setValidDoubleResult(cursor, s)
          }

          def result = resultTile

          def set(x: Int, y: Int, value: Int) =
            resultTile.set(x, y, value)

          def setDouble(x: Int, y: Int, value: Double) =
            resultTile.setDouble(x, y, value)
        }
      }
    } else {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel, target, bounds) {
          def calc(tile: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = tile.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w).toInt }
                else { s = (v * w).toInt }
              }
            }

            setValidResult(cursor, s)
          }

          def result = resultTile

          def set(x: Int, y: Int, value: Int) =
            resultTile.set(x, y, value)

          def setDouble(x: Int, y: Int, value: Double) =
            resultTile.setDouble(x, y, value)
        }
      } else {
        new KernelCalculation[Tile](tile, kernel,  target, bounds) {
          def calc(tile: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeight { (x, y, w) =>
              val v = tile.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            setValidResult(cursor, s)
          }

          def result = resultTile

          def set(x: Int, y: Int, value: Int) =
            resultTile.set(x, y, value)

          def setDouble(x: Int, y: Int, value: Double) =
            resultTile.setDouble(x, y, value)
        }
      }
    }
  }

  def apply(tile: Tile, kernel: Kernel, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, kernel, target, bounds).execute()
}
