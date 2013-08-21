package geotrellis.raster
  
import geotrellis._

trait RasterSourceBuilder[A] extends SourceBuilder[Raster,A] {
  var _dataDefinition:Op[RasterDefinition] = null

  
  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    println(s"rasterDefinition was: ${_dataDefinition}")
    val newDfn = for(tiles <- op;
    				 dfn <- _dataDefinition) yield {
      dfn.setTiles(tiles)
    }    
    this.setRasterDefinition(newDfn)
    println(s"rasterDefinition is now: ${_dataDefinition}")
    this 
  }
  /*
  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    println(s"rasterDefinition was: ${_dataDefinition}")
    this.setRasterDefinition(SetTilesOnRasterDefinition(_dataDefinition, op))
    println(s"rasterDefinition is now: ${_dataDefinition}")
    this 
  }*/

  def setRasterDefinition(dfn: geotrellis.Operation[geotrellis.RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }
}
