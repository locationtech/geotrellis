package geotrellis.engine.op.zonal

import geotrellis.engine._

package object summary {
  implicit class ZonalSummaryRasterSourceMethodExtensions(val rasterSource: RasterSource) 
      extends ZonalSummaryRasterSourceMethods { }
}
