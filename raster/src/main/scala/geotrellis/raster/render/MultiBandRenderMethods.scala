package geotrellis.raster.render

import geotrellis.raster._
import spire.syntax.cfor._

trait MultiBandRenderMethods {

  val tile: MultiBandTile

  def renderPng(): Png = {

    def resampleToByte(t: Tile, minVal: Int, maxVal: Int): Tile = {
      val byteTile = ByteArrayTile.empty(t.cols, t.rows)
      t.foreach { (col, row, z) =>
        val v = if (isData(z))
          ((z - minVal).toDouble / maxVal) * 255
        else 0

        byteTile.set(col, row, v.toInt)
      }
      byteTile
    }

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
          if (isData(r)) { ((r - rMin).toDouble / rMax).toInt * 255 }
          else 0

        val scaledG =
          if (isData(g)) { ((g - gMin).toDouble / gMax).toInt * 255 }
          else 0

        val scaledB =
          if (isData(b)) { ((b - bMin).toDouble / bMax).toInt * 255 }
          else 0

        if(scaledR + scaledG + scaledB == 0) 0
        else {
          (scaledR << 24) | (scaledG << 16) | (scaledB << 8) | 0xFF
        }
      }

    rgb.renderPng
  }
}
