package geotrellis.source

import geotrellis._

import geotrellis.raster.TileLayout

case class RasterDefinition(re:RasterExtent,tileLayout:TileLayout,tiles:Seq[Op[Raster]]) {
  def mapTiles(f:Op[Raster]=>Op[Raster]) = 
    RasterDefinition(re,tileLayout,tiles.map(f(_)))

  def setTiles(tiles:Seq[Op[Raster]]) = RasterDefinition(re, tileLayout, tiles)

}
