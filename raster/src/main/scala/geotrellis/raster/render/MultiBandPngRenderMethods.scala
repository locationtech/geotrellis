package geotrellis.raster.render

import geotrellis.raster._
import spire.syntax.cfor._

trait MultiBandPngRenderMethods {

  val tile: MultiBandTile

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
    assert(tile.bandCount == 3)
    val rgb =
      tile.convert(TypeInt).combine(0, 1, 2) { (rBand, gBand, bBand) =>
        val r = if (isData(rBand)) { rBand } else 0
        val g = if (isData(rBand)) { gBand } else 0
        val b = if (isData(rBand)) { bBand } else 0

        if(r + g + b == 0) 0
        else {
          ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | 0xFF
        }
      }

    rgb.renderPng
  }
}
