package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

trait SingleBandRasterResampleMethods extends RasterResampleMethods[SingleBandRaster] {
  def resample(target: RasterExtent, method: ResampleMethod): SingleBandRaster = {
    val (cols, rows) = (target.cols, target.rows)
    val targetTile = ArrayTile.empty(self.cellType, cols, rows)
    val targetCS = CellSize(self.extent, cols, rows)
    val resampler = Resample(method, self.tile, self.extent, targetCS)

    if(targetTile.cellType.isFloatingPoint) {
      val interpolate = resampler.resampleDouble _
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val x = target.gridColToMap(col)
          val y = target.gridRowToMap(row)
          val v = interpolate(x, y)
          targetTile.setDouble(col, row, v)
        }
      }
    } else {
      val interpolate = resampler.resample _
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val x = target.gridColToMap(col)
          val y = target.gridRowToMap(row)
          val v = interpolate(x, y)
          targetTile.set(col, row, v)
        }
      }
    }

    Raster(targetTile, target.extent)
  }
}
