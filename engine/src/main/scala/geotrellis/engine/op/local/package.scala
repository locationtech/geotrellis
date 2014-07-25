package geotrellis.engine.op

import geotrellis.engine._

package object local {
  implicit class LocalRasterSourceMethodExtensions(val rasterSource: RasterSource) 
      extends LocalRasterSourceMethods

  implicit class LocalRasterSourceSeqExtensions(val rasterSources: Traversable[RasterSource]) 
      extends LocalRasterSourceSeqMethods
}
