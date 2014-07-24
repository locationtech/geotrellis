package geotrellis.engine.op

import geotrellis.engine._

package object local {
  implicit class LocalRasterSourceMethodExtensions(val rasterSource: RasterSource) 
      extends LocalRasterSourceMethods { }
}
