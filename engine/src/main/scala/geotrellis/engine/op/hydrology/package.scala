package geotrellis.engine.op

import geotrellis.engine._

package object hydrology {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class HydrologyRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends HydrologyRasterSourceMethods
}
