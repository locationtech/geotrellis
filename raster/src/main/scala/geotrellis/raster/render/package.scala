package geotrellis.raster

package object render {
  implicit class MultiBandRenderMethodExtensions(val tile: MultiBandTile) extends MultiBandPngRenderMethods { }
}
