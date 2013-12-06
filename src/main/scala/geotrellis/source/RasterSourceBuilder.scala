package geotrellis.source
  
import geotrellis._
import geotrellis.raster._

class RasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  var _dataDefinition:Op[RasterDefinition] = null
  var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this 
  }

  def setRasterDefinition(dfn: Op[RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }

  def result = new RasterSource(_dataDefinition,_ops)

}

object RasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }

  def apply(rasterSeqSource:RasterSeqSource) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterSeqSource.rasterDefinition)
  }
}
