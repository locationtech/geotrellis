package geotrellis.raster.render

import geotrellis.raster._
import spire.syntax.cfor._

trait MultiBandRenderMethods {

  val tile: MultiBandTile

  def renderPng(): Png = {

    assert(tile.bandCount == 3)

    var (rMin, gMin, bMin) = (Int.MaxValue, Int.MaxValue, Int.MaxValue)
    var (rMax, gMax, bMax) = (Int.MinValue, Int.MinValue, Int.MinValue)

    tile.foreach { (band, z) =>
      if(isData(z)) {
        if(band == 0) {
          if(z > rMax) { rMax = z }
          if(z < rMin) { rMin = z }
        } else if(band == 1) {
          if(z > gMax) { gMax = z }
          if(z < gMin) { gMin = z }
        } else if(band == 2) {
          if(z > bMax) { bMax = z }
          if(z < bMin) { bMin = z }
        }
      }
    }

    val rgb =
      tile.convert(TypeInt).combine(0, 1, 2) { (r, g, b) =>
        val scaledR =
          if (isData(r)) { (((r - rMin).toDouble / rMax) * 255).toInt }
          else 0

        val scaledG =
          if (isData(g)) { (((g - gMin).toDouble / gMax) * 255).toInt }
          else 0

        val scaledB =
          if (isData(b)) { (((b - bMin).toDouble / bMax) * 255).toInt }
          else 0

        if(scaledR + scaledG + scaledB == 0) 0
        else {
          ((scaledR & 0xFF) << 24) | ((scaledG & 0xFF) << 16) | ((scaledB & 0xFF) << 8) | 0xFF
        }
      }

    rgb.renderPng
  }
}
