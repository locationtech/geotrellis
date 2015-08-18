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

    val imRows = tile.rows
    val imCols = tile.cols

    val redBand = tile.band(0)
    val greenBand = tile.band(1)
    val blueBand = tile.band(2)

    val redByte = resampleToByte(redBand, redBand.findMinMax._1, redBand.findMinMax._2)
    val greenByte = resampleToByte(greenBand, greenBand.findMinMax._1, greenBand.findMinMax._2)
    val blueByte = resampleToByte(blueBand, blueBand.findMinMax._1, blueBand.findMinMax._2)

    val rgb = IntArrayTile(Array.ofDim[Int](imCols * imRows), imCols, imRows)

    cfor(0)(_ < imRows, _ + 1) { row =>
      cfor(0)(_ < imCols, _ + 1) { col =>
        var v = 0
        v = {
          val r = redByte.get(col, row)
          val g = greenByte.get(col, row)
          val b = blueByte.get(col, row)
          if (r == 0 && g == 0 && b == 0) 0xFF
          else {
            val cr = if (isNoData(r)) 128 else r.toByte & 0xFF
            val cg = if (isNoData(g)) 128 else g.toByte & 0xFF
            val cb = if (isNoData(b)) 128 else b.toByte & 0xFF

            (cr << 24) | (cg << 16) | (cb << 8) | 0xFF
          }
        }
        rgb.set(col, row, v)
      }
    }
    rgb.renderPng()
  }
}