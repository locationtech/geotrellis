package geotrellis.raster.render

import geotrellis.raster.{Tile, MultibandTile}


object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandRenderMethods(val self: Tile) extends ColorMethods
    with JpgRenderMethods
    with PngRenderMethods
    with AsciiRenderMethods

  implicit class withMultibandRenderMethods(val self: MultibandTile) extends MultibandColorMethods
    with MultibandJpgRenderMethods
    with MultibandPngRenderMethods
}
