package geotrellis.engine.op

import geotrellis.engine._

package object local {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class LocalRasterSourceMethodExtensions(val rasterSource: RasterSource)
      extends LocalRasterSourceMethods

  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  implicit class LocalRasterSourceSeqExtensions(val rasterSources: Traversable[RasterSource])
      extends LocalRasterSourceSeqMethods
}
