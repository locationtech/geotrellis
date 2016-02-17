package geotrellis.engine.op

import geotrellis.engine._

package object elevation {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class ElevationRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ElevationRasterSourceMethods
}
