package geotrellis.engine.op.zonal

import geotrellis.engine._

package object summary {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class ZonalSummaryRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ZonalSummaryRasterSourceMethods { }
}
