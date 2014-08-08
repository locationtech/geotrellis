package geotrellis.engine.op

import geotrellis.engine._

package object elevation {
  implicit class ElevationRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends ElevationRasterSourceMethods
}
