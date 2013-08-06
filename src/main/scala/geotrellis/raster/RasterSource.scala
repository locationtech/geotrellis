package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._

/*class RasterSource(seqOp: Op[Seq[Op[Raster]]]) extends DataSource[Raster] with RasterSourceLike[RasterSource] {
  def tiles = partitions

  def partitions(): Op[Seq[Op[Raster]]] = seqOp

}*/

trait RasterSource extends DataSource[Raster] {
	

}

/*
object RasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[DataSource[Raster], Raster, RasterSource] = new CanBuildSourceFrom[DataSource[Raster], Raster, RasterSource] {
    def apply() = new RasterSourceBuilder
    def apply(from: DataSource[Raster], op: Op[Seq[Op[Raster]]]) = {
      val builder = new RasterSourceBuilder
      builder.op = op
      builder
    }
  }
}*/