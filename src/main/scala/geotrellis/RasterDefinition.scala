package geotrellis

import geotrellis.raster.TileLayout

//trait DataDefinition[T] {
  
//}

case class RasterDefinition(re:RasterExtent,tileLayout:TileLayout,tiles:Seq[Op[Raster]]) {
  def mapTiles(f:Op[Raster]=>Op[Raster]) = 
    RasterDefinition(re,tileLayout,tiles.map(f(_)))
}