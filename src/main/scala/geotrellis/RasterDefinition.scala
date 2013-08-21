package geotrellis

import geotrellis.raster.TileLayout

//trait DataDefinition[T] {
  
//}

//sealed abstract class RasterDefinition

//object RasterDefinition {
//  def apply(re:RasterExtent,tileLayout:TileLayout,tiles:Seq[Op[Raster]])
//}

case class RasterDefinition(re:RasterExtent,tileLayout:TileLayout,tiles:Seq[Op[Raster]]) {
  def mapTiles(f:Op[Raster]=>Op[Raster]) = 
    RasterDefinition(re,tileLayout,tiles.map(f(_)))

  def setTiles(tiles:Seq[Op[Raster]]) = RasterDefinition(re, tileLayout, tiles)

}
