package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

class NearestNeighborResample(tile: Tile, extent: Extent)
    extends Resample(tile, extent) {

  override def resampleValid(x: Double, y: Double): Int = {
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

  override def resampleDoubleValid(x: Double, y: Double): Double = {
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
