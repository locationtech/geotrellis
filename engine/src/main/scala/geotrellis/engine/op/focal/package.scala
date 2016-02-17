package geotrellis.engine.op

import geotrellis.engine._

package object focal {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class FocalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends FocalRasterSourceMethods
}
