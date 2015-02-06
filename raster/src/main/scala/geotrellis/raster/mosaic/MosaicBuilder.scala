package geotrellis.raster.mosaic

import geotrellis.raster._
import geotrellis.vector.Extent

class MosaicBuilder(cellType: CellType, extent: Extent, cols: Int, rows: Int) {
  val tile = ArrayTile.empty(cellType, cols, rows)

  def +=(raster: Raster): Unit = {
    if(extent == raster.extent && cols == raster.cols && rows == raster.rows) {
      tile.merge(raster.tile)
    } else {
      tile.merge(extent, raster.extent, raster.tile)
    }
  }

  def result = Raster(tile, extent)
}
