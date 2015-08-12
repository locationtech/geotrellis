package geotrellis.raster.interpolation

import geotrellis.raster.{DoubleArrayTile, RasterExtent, Tile}
import geotrellis.vector.Extent

object Interpolation {
  /**
   * Interpolation for a Tile
   * @param tile        Tile to be interpolated
   * @return            Tile set with the interpolated values
   */
  def apply(tile: Tile, extent: Extent)(predictor: (Double, Double) => Double): Tile = {
    val numberOfCells = tile.cols * tile.rows
    val rasterExtent = RasterExtent(tile, extent)
    val result = DoubleArrayTile.empty(tile.cols, tile.rows)

    cfor(0)(_ < tile.cols, _ + 1) { col =>
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        val (x, y) = rasterExtent.gridToMap(col, row)
        val prediction = (x, y)
        result.setDouble(col, row, prediction)
      }
    }

    result
  }

  def kriging(tile: Tile, extent: Extent)(kriging: Kriging): Tile =
    apply(tile, extent) { (x: Double, y: Double) => kriging(x, y)._1 }
}
