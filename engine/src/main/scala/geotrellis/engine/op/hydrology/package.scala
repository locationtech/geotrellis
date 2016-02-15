package geotrellis.engine.op

import geotrellis.engine._

package object hydrology {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class HydrologyRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends HydrologyRasterSourceMethods
}
