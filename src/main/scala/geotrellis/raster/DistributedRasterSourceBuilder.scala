package geotrellis.raster

import geotrellis._

class DistributedRasterSourceBuilder extends RasterSourceBuilder[DistributedRasterSource] {
  def result = new DistributedRasterSource(_dataDefinition)
}

object DistributedRasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new DistributedRasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}
