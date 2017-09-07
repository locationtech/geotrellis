package geotrellis.raster.render.ascii

import geotrellis.raster.Tile
import geotrellis.raster.render.{ColorMap, ColorRamp}
import spire.syntax.cfor.cfor

/**
 * Routines for converting tiles into ASCII art based on cell values.
 *
 * @author sfitch
 * @since 9/6/17
 */
object AsciiArtEncoder {

  def encode(tile: Tile, settings: Settings): String = {
    val palette = settings.palette
    val palSize = palette.length
    val colorMap = if(tile.cellType.isFloatingPoint) {
     val hist = tile.histogramDouble(palSize)
     ColorMap.fromQuantileBreaks(hist, palette.colorRamp)
    }
    else {
     val hist = tile.histogram
     ColorMap.fromQuantileBreaks(hist, palette.colorRamp)
    }

    val intEncoded = colorMap.render(tile)

    val sb = new StringBuilder
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val p = intEncoded.get(col, row)
        sb += p.toChar
      }
      sb += '\n'
    }
    //sb.dropRight(1)
    sb.toString
  }

  case class Settings(palette: Palette = Palette.WIDE)

  case class Palette(values: Array[Char]) {
    def colorRamp: ColorRamp = ColorRamp(values.map(_.toInt))
    def length: Int = values.length
  }

  object Palette {
    def apply(str: String): Palette = apply(str.toCharArray)
    val WIDE = Palette(" .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$")
    val NARROW = Palette( " .:-=+*#%@")
    val HATCHING = Palette("    ...,,;;---===+++xxxXX##")
    val FILLED = Palette(" ▤▦▩█")
    val STIPLED = Palette(" ▖▚▜█")
    val BINARY = Palette(" #")
  }
}
