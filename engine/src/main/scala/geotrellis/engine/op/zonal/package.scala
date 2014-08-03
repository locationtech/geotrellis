package geotrellis.engine.op

import geotrellis.engine._

package object zonal {
  implicit class ZonalRasterSourceMethodExtensions(val rasterSource: RasterSource) 
      extends ZonalRasterSourceMethods { }
}
