package geotrellis.engine.op

import geotrellis.engine._

package object zonal {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class ZonalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ZonalRasterSourceMethods { }
}
