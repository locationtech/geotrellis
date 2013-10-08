
package geotrellis.source

import geotrellis._

import geotrellis.raster.TileLayout

case class RasterDefinition(layerName:String,re:RasterExtent,tileLayout:TileLayout)
