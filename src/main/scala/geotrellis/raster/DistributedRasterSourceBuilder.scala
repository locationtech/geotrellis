package geotrellis.raster

import geotrellis._

class DistributedRasterSourceBuilder extends RasterSourceBuilder[DistributedRasterSource] {
/*  var _dataDefinition:Op[RasterDefinition] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    println(s"rasterDefinition was: ${_dataDefinition}")
    val newDfn = op.flatMap ( tiles => {
      _dataDefinition.map(
        (dfn) => {
          dfn.setTiles(tiles)
        }
      )
    })
    
    this.setRasterDefinition(newDfn)
    println(s"rasterDefinition is now: ${_dataDefinition}")
    this 
  }
  
  def setRasterDefinition(dfn: geotrellis.Operation[geotrellis.RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }
*/
  def result = new DistributedRasterSource(_dataDefinition)

}

object DistributedRasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new DistributedRasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}
