package geotrellis.source
  
import geotrellis._
import geotrellis.raster._

class RasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  var _dataDefinition:Op[RasterDefinition] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    this.setRasterDefinition(SetTilesOnRasterDefinition(_dataDefinition, op))
    this 
  }

  def setRasterDefinition(dfn: Operation[RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }

  def result = new RasterSource(_dataDefinition)

}

case class SetTilesOnRasterDefinition(rasterDefinition:Op[RasterDefinition], opSeq:Op[Seq[Op[Raster]]])
       extends Op2(rasterDefinition,opSeq) ({
         (dfn, opSeq) =>  Result(dfn.setTiles(opSeq))
       })

object RasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}
