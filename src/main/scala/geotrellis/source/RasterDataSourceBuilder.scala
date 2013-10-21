package geotrellis.source
  
import geotrellis._
import geotrellis.raster._

class RasterDataSourceBuilder extends SourceBuilder[Raster,RasterDataSource] {
  var _dataDefinition:Op[RasterDefinition] = null
  var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this 
  }

  def setRasterDefinition(dfn: Operation[RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }

  def result = new RasterDataSource(_dataDefinition,_ops)

}

object RasterDataSourceBuilder {
  def apply(rasterSource:RasterDataSource) = {
    val builder = new RasterDataSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}
