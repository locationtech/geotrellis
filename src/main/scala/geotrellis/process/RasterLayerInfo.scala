package geotrellis.process

import geotrellis._

case class RasterLayerInfo(name:String,
                           rasterType:RasterType,
                           rasterExtent:RasterExtent,
                           epsg:Int,
                           xskew:Double,
                           yskew:Double,
                           shouldCache:Boolean = false)

