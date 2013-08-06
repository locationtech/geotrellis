package geotrellis

import geotrellis.raster.TileLayout

trait DataDefinition

case class RasterDefinition(re:RasterExtent,tileLayout:TileLayout,tiles:Seq[Op[Raster]]) extends DataDefinition {
  def mapTiles(f:Op[Raster]=>Op[Raster]) = 
    RasterDefinition(re,tileLayout,tiles.map(f(_)))
}