package geotrellis.engine.op

import geotrellis.engine._

package object zonal {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class ZonalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ZonalRasterSourceMethods { }
}
