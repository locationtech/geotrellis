package geotrellis.engine.op

import geotrellis.engine._

package object global {
  implicit class GlobalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends GlobalRasterSourceMethods
}
