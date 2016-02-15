package geotrellis.engine.op

import geotrellis.engine._

package object local {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class LocalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends LocalRasterSourceMethods

  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  implicit class LocalRasterSourceSeqExtensions(val rasterSources: Traversable[RasterSource])
      extends LocalRasterSourceSeqMethods
}
