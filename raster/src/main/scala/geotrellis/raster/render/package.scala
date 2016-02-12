package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  implicit class RenderMethodExtensions(val tile: Tile) extends ColorMethods with PngRenderMethods with JpgRenderMethods { }
  implicit class MultiBandRenderMethodExtensions(val tile: MultiBandTile) extends MultiBandPngRenderMethods { }
}
