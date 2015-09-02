package geotrellis.raster

package object render {
  implicit class RenderMethodExtensions(val tile: Tile) extends RenderMethods { }
  implicit class MultiBandRenderMethodExtensions(val tile: MultiBandTile) extends MultiBandRenderMethods { }
}
