package geotrellis.raster

package object render {
  implicit class RenderMethodExtensions(val tile: Tile) extends ColorMethods with PngRenderMethods with JpgRenderMethods { }
  implicit class MultibandRenderMethodExtensions(val tile: MultibandTile) extends MultibandPngRenderMethods { }
}
