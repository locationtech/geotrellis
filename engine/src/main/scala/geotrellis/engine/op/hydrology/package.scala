package geotrellis.engine.op

import geotrellis.engine._

package object hydrology {
  implicit class HydrologyRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends HydrologyRasterSourceMethods
}
