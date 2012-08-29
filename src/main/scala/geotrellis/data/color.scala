package geotrellis.data

import geotrellis._

object Color {
  // read color bands from a color value
  @inline final def unzipR(x:Int) = (x >> 24) & 0xff
  @inline final def unzipG(x:Int) = (x >> 16) & 0xff
  @inline final def unzipB(x:Int) = (x >> 8) & 0xff
  @inline final def unzipA(x:Int) = x & 0xff

  @inline final def isOpaque(x:Int) = unzipA(x) == 255
  @inline final def isTransparent(x:Int) = unzipA(x) == 0

  @inline final def isGrey(x:Int) = {
    Color.unzipR(x) == Color.unzipG(x) && Color.unzipG(x) == Color.unzipB(x)
  }

  // split one color value into three color band values plus an alpha band
  final def unzip(x:Int) = (unzipR(x), unzipG(x), unzipB(x), unzipA(x))

  // combine three color bands into one color value
  @inline final def zip(r:Int, g:Int, b:Int, a:Int) = (r << 24) + (g << 16) + (b << 8) + a
}

sealed trait Colors {
  def getColors(n:Int):Array[Int]
}

case class RgbaPalette(cs:Array[Int]) extends Colors {
  def getColors(n:Int) = sys.error("")
}

case class RgbaColors(cs:Array[Int]) extends Colors {
  def getColors(n:Int) = sys.error("")
}

/**
 * Mapper which wraps color breaks and 
 */
case class ColorMapper(cb:ColorBreaks, nodataColor:Int) extends Function1[Int, Int] {
  def apply(z:Int):Int = if (z == NODATA) nodataColor else cb.get(z)
}

/**
 * Data object to represent class breaks, stored as an array of ranges.
 * Each individual range is stored as a tuple of (max value, color)
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
  def getColors(n:Int):Array[Int]

  // returns a string of hex colors: "ff0000,00ff00,0000ff"
  def getColorString(n:Int) = getColors(n).map("%06X" format _).mkString(",")
}

/**
 * Abstract class for generating a range of colors. 
 */
abstract class ColorRangeChooser extends ColorChooser {
  // meant to be used with single numbesr, not RGB values
  def getRanges(masker:(Int) => Int, num:Int):Array[Int]

  // returns a sequence of integers
  def getColors(n:Int):Array[Int] = {
    val rs = getRanges(Color.unzipR, n)
    val gs = getRanges(Color.unzipG, n)
    val bs = getRanges(Color.unzipB, n)
    val as = getRanges(Color.unzipA, n)

    val colors = new Array[Int](n)
    var i = 0
    while (i < n) {
      colors(i) = Color.zip(rs(i), gs(i), bs(i), as(i))
      i += 1
    }
    colors
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
      Array(start)
    } else {
      val ranges = new Array[Int](n)
      var i = 0
      while (i < n) {
        ranges(i) = Blender.blend(start, end, i, n - 1)
        i += 1
      }
      ranges
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

    if (count < 2) {
      Array(hues(0))
    } else {
      val ranges = new Array[Int](count)
      var i = 0
      while (i < count) {
        val j = (i * mult) / denom
        ranges(i) = if (j < mult) {
          Blender.blend(hues(j), hues(j + 1), (i * mult) % denom, denom)
        } else {
          hues(j)
        }
        i += 1
      }
      ranges
    }
  }
}
