package geotrellis.engine.op

import geotrellis.engine._

package object global {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class GlobalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends GlobalRasterSourceMethods
}
