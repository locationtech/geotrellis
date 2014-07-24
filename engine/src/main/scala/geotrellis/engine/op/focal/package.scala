package geotrellis.engine.op

import geotrellis.engine._

package object focal {
  implicit class FocalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends FocalRasterSourceMethods
}
