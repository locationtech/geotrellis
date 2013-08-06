package geotrellis.raster

import geotrellis._

class DistributedRasterSourceBuilder extends RasterSourceBuilder[DistributedRasterSource] {
  def result = new DistributedRasterSource(null)
  var _dataDefinition:Op[RasterDefinition] = null

/** As seen from class DistributedRasterSourceBuilder, the missing signatures are as follows.  
 *  *  For convenience, these are usable as stub implementations.  */  
  def setDataDefinition(dfn: geotrellis.Operation[geotrellis.DataDefinition]): this.type = { this }  
  def setOp(op: Seq[geotrellis.Operation[geotrellis.Raster]]): this.type = { this } 	
  def setRasterDefinition(dfn: geotrellis.Operation[geotrellis.RasterDefinition]): this.type = this 
}