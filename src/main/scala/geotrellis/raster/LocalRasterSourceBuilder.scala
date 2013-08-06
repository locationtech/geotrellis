package geotrellis.raster

import geotrellis._


class LocalRasterSourceBuilder extends SourceBuilder[Raster, LocalRasterSource] {
  var _dataDefinition:Op[RasterDefinition] = null

  def setOp(op: Op[Seq[geotrellis.Operation[geotrellis.Raster]]]): this.type = {
    this.op = op
    this 
  }
  
  def setRasterDefinition(dfn: geotrellis.Operation[geotrellis.RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }
  
  def result = LocalRasterSource(_dataDefinition)
}

object LocalRasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new LocalRasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}