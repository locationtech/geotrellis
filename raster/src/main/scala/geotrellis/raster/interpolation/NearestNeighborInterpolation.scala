package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

class NearestNeighborInterpolation(tile: Tile, extent: Extent)
    extends Interpolation(tile, extent) {

  override def interpolateValid(x: Double, y: Double): Int = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)
    tile.get(col, row)
  }

  override def interpolateDoubleValid(x: Double, y: Double): Double = {
    val col = re.mapXToGrid(x)
    val row = re.mapYToGrid(y)
    tile.getDouble(col, row)
  }

}
