package geotrellis.raster

import geotrellis._


class LocalRasterSourceBuilder extends RasterSourceBuilder[LocalRasterSource] {  
  def result = LocalRasterSource(_dataDefinition)
}

object LocalRasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new LocalRasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}


  
  
case class SetTilesOnRasterDefinition(rasterDefinition:Op[RasterDefinition], opSeq:Op[Seq[Op[Raster]]]) 
	extends Op2(rasterDefinition,opSeq) ({
	  (dfn, opSeq) =>  Result(dfn.setTiles(opSeq))
	})
