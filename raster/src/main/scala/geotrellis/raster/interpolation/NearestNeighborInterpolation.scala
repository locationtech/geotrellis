package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

class NearestNeighborInterpolation(tile: Tile, extent: Extent)
    extends Interpolation(tile, extent) {

  override def interpolateValid(x: Double, y: Double): Int = {
    val col = {
      val c = re.mapXToGrid(x)
      if(c < tile.cols) c else tile.cols - 1
    }

    val row = {
      val r = re.mapYToGrid(y)
      if(r < tile.rows) r else tile.rows - 1
    }

    tile.get(col, row)
  }

  override def interpolateDoubleValid(x: Double, y: Double): Double = {
    val col = {
      val c = re.mapXToGrid(x)
      if(c < tile.cols) c else tile.cols - 1
    }

    val row = {
      val r = re.mapYToGrid(y)
      if(r < tile.rows) r else tile.rows - 1
    }

    tile.getDouble(col, row)
  }

}
