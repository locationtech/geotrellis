package geotrellis.raster

package object render {
  implicit class RenderMethodExtensions(val tile: Tile) extends RenderMethods { }
}
