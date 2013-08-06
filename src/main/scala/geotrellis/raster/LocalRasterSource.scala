package geotrellis.raster

import geotrellis._

object LocalRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] = new CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] {
    def apply() = new LocalRasterSourceBuilder
    def apply(from: LocalRasterSource, op: Op[Seq[Op[Raster]]]) = new LocalRasterSourceBuilder
  }
}
class LocalRasterSourceBuilder extends SourceBuilder[Raster, LocalRasterSource] {
  def result = new LocalRasterSource(null)
}

class LocalRasterSource(seqOp: Op[Seq[Op[Raster]]]) extends RasterSource(seqOp) with RasterSourceLike[LocalRasterSource] {}
