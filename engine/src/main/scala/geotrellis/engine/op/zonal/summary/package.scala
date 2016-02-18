package geotrellis.engine.op.zonal

import geotrellis.engine._

package object summary {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class ZonalSummaryRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ZonalSummaryRasterSourceMethods { }
}
