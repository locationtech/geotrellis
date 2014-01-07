package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId
import geotrellis.raster.TileLayout

case class RasterDefinition(layerId:LayerId,
                            rasterExtent:RasterExtent,
                            tileLayout:TileLayout,
                            rasterType:RasterType,
                            catalogued:Boolean = true) {
  def isTiled = tileLayout.isTiled

  def withType(newType:RasterType) = 
    RasterDefinition(layerId, rasterExtent, tileLayout, newType)
}

object RasterDefinition {
  def fromRaster(r: Raster): RasterDefinition = 
    RasterDefinition(
      LayerId.MEM_RASTER,
      r.rasterExtent,
      TileLayout.singleTile(r.rasterExtent),
      r.rasterType,
      false)
}
