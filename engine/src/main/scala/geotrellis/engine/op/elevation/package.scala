package geotrellis.engine.op

import geotrellis.engine._

package object elevation {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class ElevationRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ElevationRasterSourceMethods
}
