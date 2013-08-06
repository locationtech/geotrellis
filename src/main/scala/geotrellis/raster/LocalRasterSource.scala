package geotrellis.raster

import geotrellis._
import geotrellis.raster.op.tiles.GetTileOps

object LocalRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] = new CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] {
    def apply() = new LocalRasterSourceBuilder
    def apply(rasterSrc:LocalRasterSource) = LocalRasterSourceBuilder(rasterSrc)
  }
}

case class LocalRasterSource(val rasterDefinition:Op[RasterDefinition]) extends RasterSource
  with RasterSourceLike[LocalRasterSource] {
  def partitions = rasterDefinition.map(_.tiles)
  def converge = ???
}
