package geotrellis.engine

import geotrellis.engine._

package object stats {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class StatsRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends StatsRasterSourceMethods { }
}
