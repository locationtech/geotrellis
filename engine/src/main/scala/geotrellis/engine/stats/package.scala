package geotrellis.engine

import geotrellis.engine._

package object stats {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class StatsRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends StatsRasterSourceMethods { }
}
