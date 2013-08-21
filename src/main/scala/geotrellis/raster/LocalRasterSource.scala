package geotrellis.raster

import geotrellis._
import geotrellis.raster.op.tiles.GetTileOps

object LocalRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] = new CanBuildSourceFrom[LocalRasterSource, Raster, LocalRasterSource] {
    def apply() = new LocalRasterSourceBuilder
    def apply(rasterSrc:LocalRasterSource) = LocalRasterSourceBuilder(rasterSrc)
  }

  def fromRaster(raster:Op[Raster]) = {
    val rasterDef = RasterDefinitionFromRaster(raster)
    new LocalRasterSource(rasterDef)
  }
}

case class LocalRasterSource(val rasterDefinition:Op[RasterDefinition]) extends RasterSource  with RasterSourceLike[LocalRasterSource] {
  //TODO: move to RasterSource{Like}?
  def partitions = GetTilesFromRasterDefinition(rasterDefinition)//rasterDefinition.map(_.tiles)
}

case class GetTilesFromRasterDefinition(dfn:Op[RasterDefinition]) extends Op1(dfn)({d => Result(d.tiles)})

case class RasterDefinitionFromRaster(r:Op[Raster]) extends Op1(r) ({
  (r) => {
	val data = r.data.asTiledRasterData(r)
	  	
	val dfn = RasterDefinition(
			r.rasterExtent,
			data.tileLayout,
			data.getTileOpList(r.rasterExtent)
    )
    Result(dfn)
  }
})
