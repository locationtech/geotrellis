package geotrellis.raster.render

import geotrellis.raster._
import spire.syntax.cfor._

trait MultiBandJpgRenderMethods {

  val tile: MultiBandTile

  def renderJpg(): Jpg = {
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

    rgb.renderJpg
  }
}
