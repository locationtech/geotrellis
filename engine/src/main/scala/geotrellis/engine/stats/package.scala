package geotrellis.engine

import geotrellis.engine._

package object stats {
  implicit class StatsRasterSourceMethodExtensions(val rasterSource: RasterSource) 
      extends StatsRasterSourceMethods { }
}
