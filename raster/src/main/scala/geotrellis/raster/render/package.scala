package geotrellis.raster

package object render {
  implicit class RenderMethodExtensions(val tile: Tile) extends SharedRenderMethods with PngRenderMethods with JpgRenderMethods { }
  implicit class MultiBandRenderMethodExtensions(val tile: MultiBandTile) extends MultiBandPngRenderMethods { }
}
