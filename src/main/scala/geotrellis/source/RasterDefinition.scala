
package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId

import geotrellis.raster.TileLayout

case class RasterDefinition(layerId:LayerId,re:RasterExtent,tileLayout:TileLayout)
