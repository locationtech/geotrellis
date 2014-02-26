package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId
import geotrellis.raster.TileLayout

class RasterDefinition(val layerId: LayerId,
                       val rasterExtent: RasterExtent,
                       val tileLayout: TileLayout,
                       val rasterType: RasterType,
                       val catalogued: Boolean) {
  def isTiled = tileLayout.isTiled

  def withType(newType: RasterType) =
    new RasterDefinition(layerId, rasterExtent, tileLayout, newType, true)
}

object RasterDefinition {

  def apply(layerId: LayerId, re: RasterExtent, tl: TileLayout, rt: RasterType, catalogued: Boolean = true) = 
    new RasterDefinition(layerId, re, tl, rt, catalogued)

  def fromRaster(r: Raster): RasterDefinition =
    new RasterDefinition(
      LayerId.MEM_RASTER,
      r.rasterExtent,
      TileLayout.singleTile(r.rasterExtent),
      r.rasterType,
      false)
}
