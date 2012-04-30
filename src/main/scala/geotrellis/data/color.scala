package geotrellis.data

import geotrellis._

object Color {
  // read color bands from a color value
  final def unzipR(x:Int) = (x >> 16) & 0xff
  final def unzipG(x:Int) = (x >> 8) & 0xff
  final def unzipB(x:Int) = (x) & 0xff

  // split one color value into three color band values
  final def unzipRGB(x:Int) = (unzipR(x), unzipG(x), unzipB(x))

  // combine three color bands into one color value
  final def zipRGB(r:Int, g:Int, b:Int) = (r << 16) + (g << 8) + b
}

/**
 * Mapper which wraps color breaks and 
 */
case class ColorMapper(cb:ColorBreaks, nodataColor:Int) extends Function1[Int, Int] {
  def apply(z:Int):Int = if (z == NODATA) nodataColor else cb.get(z)
}

/**
 * Data object to represent class breaks, stored as an array of ranges.
 * Each individual range is stored as a tuple of (min value, max value)
 */
case class ColorBreaks(breaks:Array[(Int, Int)]) {
  def firstColor = breaks(0)._2
  def lastColor = breaks(breaks.length - 1)._2
  def get(z:Int):Int = {
    breaks.foreach { case (limit, color) => if (z <= limit) return color }
    lastColor
  }
  override def toString = {
    "ColorBreaks(%s)" format breaks.map(t => "(%s, %06x)" format (t._1, t._2)).mkString(", ")
  }
}

/**
 * Color blender used for generating symbology for class breaks.
 */
object Blender {
  def blend(start:Int, end:Int, numerator:Int, denominator:Int) = {
    start + (((end - start) * numerator) / denominator)
  }
}

/**
 * Abstract class for generating colors for class breaks. 
 */
abstract class ColorChooser {
  // get a sequence of n colors
  def getColors(n:Int):Seq[Int]

  // returns a string of hex colors: "ff0000,00ff00,0000ff"
  def getColorString(n:Int) = getColors(n).map("%06X" format _).mkString(",")
}

/**
 * Abstract class for generating a range of colors. 
 */
abstract class ColorRangeChooser extends ColorChooser {
  // meant to be used with single numbesr, not RGB values
  def getRanges(masker:(Int) => Int, num:Int):Seq[Int]

  // returns a sequence of integers
  def getColors(n:Int):Seq[Int] = {
    val rs = getRanges(Color.unzipR _, n)
    val gs = getRanges(Color.unzipG _, n)
    val bs = getRanges(Color.unzipB _, n)

    (0 until n).map(i => Color.zipRGB(rs(i), gs(i), bs(i)))
  }

}

/**
 * Generates colors between an initial color and an end color.
 */
case class LinearColorRangeChooser(color1:Int, color2:Int) extends ColorRangeChooser {
  def getRanges(masker:(Int) => Int, n:Int) = {
    val start = masker(color1)
    val end   = masker(color2)
    if (n < 2) {
      List(start)
    } else {
      (0 until n).map { i => Blender.blend(start, end, i, n - 1) }.toList
    }
  }

}


/**
 * Generates a range of colors from an array of initial colors. 
 */
case class MultiColorRangeChooser(colors:Array[Int]) extends ColorRangeChooser {
  def getRanges(masker:(Int) => Int, count:Int) = {
    val hues = colors.map(masker)
    val mult = colors.length - 1
    val denom = count - 1

    (0 until count).map {
      i => if (denom < 1) {
        hues(0)
      } else {
        val j = (i * mult) / denom
        if (j < mult) {
          Blender.blend(hues(j), hues(j + 1), (i * mult) % denom, denom)
        } else {
          hues(j)
        }
      }
    }.toList
  }
}
