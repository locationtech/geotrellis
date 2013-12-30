
package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId

import geotrellis.raster.TileLayout

case class RasterDefinition(layerId:LayerId,
                            rasterExtent:RasterExtent,
                            tileLayout:TileLayout,
                            rasterType:RasterType) {
  def isTiled = tileLayout.isTiled

  def withType(newType:RasterType) = 
    RasterDefinition(layerId, rasterExtent, tileLayout, newType)
}

