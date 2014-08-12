package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

class NearestNeighborInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  private val re = RasterExtent(tile, extent)
  private val cols = tile.cols
  private val rows = tile.rows

  def interpolate(x: Double, y: Double): Int = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)

    if(0 <= col && col < cols && 0 <= row && row < rows)
      tile.get(col, row)
    else
      NODATA
  }

  def interpolateDouble(x: Double, y: Double): Double = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)

    if(0 <= col && col < cols && 0 <= row && row < rows)
      tile.getDouble(col, row)
    else  
      Double.NaN
  }

}
