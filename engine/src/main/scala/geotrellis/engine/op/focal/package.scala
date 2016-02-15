package geotrellis.engine.op

import geotrellis.engine._

package object focal {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class FocalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends FocalRasterSourceMethods
}
