package geotrellis.raster

import geotrellis._
import geotrellis.raster.op.tiles.GetTileOps

object LocalRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] = new CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] {
    def apply() = new LocalRasterSourceBuilder
    def apply(dfn:RasterDefinition, op: Op[Seq[Op[Raster]]]) = new LocalRasterSourceBuilder().setOp(op).setRasterDefinition(dfn)
  }
}
class LocalRasterSourceBuilder extends SourceBuilder[Raster, LocalRasterSource] {
  val dataDefinition = ???
  def result = new LocalRasterSource(null)
  def setDataDefinition(dfn: geotrellis.Operation[geotrellis.DataDefinition]): this.type = this 
  def setOp(op: Seq[geotrellis.Operation[geotrellis.Raster]]): this.type = this 
  def setRasterDefinition(dfn: geotrellis.Operation[geotrellis.RasterDefinition]): this.type = this 
}

class LocalRasterSource(val raster:Op[Raster])
  extends RasterSourceLike[LocalRasterSource] {
  def dataDefinition = ???
  def partitions = GetTileOps(raster)

  def converge = ???
  

}
