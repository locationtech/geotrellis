package geotrellis.engine.op

import geotrellis.engine._

package object global {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class GlobalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends GlobalRasterSourceMethods
}
