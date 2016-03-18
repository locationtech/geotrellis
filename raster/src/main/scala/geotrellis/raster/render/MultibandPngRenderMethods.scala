package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._

trait MultibandPngRenderMethods extends MethodExtensions[MultibandTile] {

  /**
    * Generate a PNG image from a multiband raster.
    *
    * Use this operation when you have a multiband raster of data that you want to
    * visualize with an image.
    *
    * To render with this method, you must first ensure that your tile is encoded
    * with integer data whose values range from 0 to 255.
    */
  def renderPng(): Png = {
    self.color().renderPng
  }
}
