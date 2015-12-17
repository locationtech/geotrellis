package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._

package object resample {
  trait ResampleMethods extends TileMethods {
    def resample(source: Extent, target: RasterExtent): Tile =
      resample(source, target, ResampleMethod.DEFAULT)

    def resample(source: Extent, target: Extent): Tile =
      resample(source, target, ResampleMethod.DEFAULT)

    def resample(extent: Extent, targetExtent: RasterExtent, method: ResampleMethod): Tile = {
      val resampler = Resample(method, tile, extent, targetExtent.cellSize)
      val (cols, rows) = (targetExtent.cols, targetExtent.rows)
      val targetTile = ArrayTile.empty(tile.cellType, cols, rows)

      if(targetTile.cellType.isFloatingPoint) {
        val interpolate = resampler.resampleDouble _
        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            val x = targetExtent.gridColToMap(col)
            val y = targetExtent.gridRowToMap(row)
            val v = interpolate(x, y)
            targetTile.setDouble(col, row, v)
          }
        }
      } else {
        val interpolate = resampler.resample _
        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            val x = targetExtent.gridColToMap(col)
            val y = targetExtent.gridRowToMap(row)
            println("interpolating with", x, y)
            val v = interpolate(x, y)
            targetTile.set(col, row, v)
          }
        }
      }
      targetTile
    }

    def resample(extent: Extent, targetExtent: Extent, method: ResampleMethod): Tile =
      resample(extent, RasterExtent(extent, tile.cols, tile.rows).createAligned(targetExtent), method)

    def resample(extent: Extent, targetCols: Int, targetRows: Int): Tile =
      resample(extent, targetCols, targetRows, ResampleMethod.DEFAULT)

    def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): Tile =
      resample(extent, RasterExtent(extent, targetCols, targetRows), method)

    /** Only changes the resolution */
    def resample(targetCols: Int, targetRows: Int): Tile =
      resample(Extent(0.0, 0.0, 1.0, 1.0), targetCols, targetRows)

  }

  implicit class ResampleExtensions(val tile: Tile) extends ResampleMethods {}
}
