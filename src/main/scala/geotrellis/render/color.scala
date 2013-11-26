package geotrellis.render

import scala.math.round

import geotrellis._

/**
 * All colors in geotrellis are encoded as RGBA integer values.
 *
 * This object provides utility methods that operate on RGBA integer values.
 */ 
object Color {
  // Get red color band from RGBA color value.
  @inline final def unzipR(x:Int) = (x >> 24) & 0xff

  // Get green color band from RGBA color value.
  @inline final def unzipG(x:Int) = (x >> 16) & 0xff

  // Get blue color band from RGBA color value.
  @inline final def unzipB(x:Int) = (x >> 8) & 0xff

  // Get alpha band from RGBA color balue.
  @inline final def unzipA(x:Int) = x & 0xff

  // Returns true if the alpha of this color is 255 (opaque).
  @inline final def isOpaque(x:Int) = unzipA(x) == 255
  
  // Returns true if the alpha of this color is 0 (100% transparent).
  @inline final def isTransparent(x:Int) = unzipA(x) == 0

  // Returns true if red, blue, and green band are equal.
  @inline final def isGrey(x:Int) = {
    Color.unzipR(x) == Color.unzipG(x) && Color.unzipG(x) == Color.unzipB(x)
  }

  // split one color value into three color band values plus an alpha band
  final def unzip(x:Int) = (unzipR(x), unzipG(x), unzipB(x), unzipA(x))

  // combine three color bands into one color value
  @inline final def zip(r:Int, g:Int, b:Int, a:Int) = (r << 24) + (g << 16) + (b << 8) + a

  // convert an RGB color integer to an opaque RGBA color integer
  def rgbToRgba(rgb:Int) = { (rgb << 8) + 0xff }

  /**
   * This method is used for cases in which we are provided with a different
   * number of colors than we need.  This method will return a smaller list
   * of colors the provided list of colors, spaced out amongst the provided
   * color list.  
   *
   * For example, if we are provided a list of 9 colors on a red
   * to green gradient, but only need a list of 3, we expect to get back a 
   * list of 3 colors with the first being red, the second color being the 5th
   * color (between red and green), and the last being green.
   *
   * @param colors  Provided RGBA color values
   * @param n       Length of list to return 
   */
  def spread(colors:Array[Int], n:Int): Array[Int] = {
    if (colors.length == n) return colors

    val colors2 = new Array[Int](n)
    colors2(0) = colors(0)
  
    val b = n - 1
    val c = colors.length - 1
    var i = 1
    while (i < n) {
      colors2(i) = colors(round(i.toDouble * c / b).toInt)
      i += 1
    }

    colors2
  }
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
 * Mapper which wraps color breaks and nodataColor
 */
case class ColorMapper(cb:ColorBreaks, nodataColor:Int) extends Function1[Int, Int] {
  def apply(z:Int):Int = if (isNoData(z)) nodataColor else cb.get(z)
}

/**
 * ColorBreaks describes a way to render a raster into a colored image.
 *
 * This class defines a set of value ranges and assigns a color to
 * each value range.
 *
 * @param limits  An array with the maximum value of each range
 * @param colors  An array with the color assigned to each range
 */
case class ColorBreaks(limits:Array[Int], colors:Array[Int]) {
  assert(limits.length == colors.length)
  assert(colors.length > 0)

  val lastColor = colors(colors.length - 1)

  def length = limits.length

  def get(z:Int):Int = {
    var i = 0
    val last = colors.length - 1
    while (i < last) {
      if (z <= limits(i)) return colors(i)
      i += 1
    }
    lastColor
  }

  override def toString = "ColorBreaks(%s, %s)" format (
    limits.mkString("Array(", ", ", ")"),
    colors.map("%08x" format _).mkString("Array(", ", ", ")")
  )
}

object ColorBreaks {
  /**
   * This method is used for cases in which we are provided with a different
   * number of colors than we have value ranges.  This method will return a 
   * return a ClassBreak object where the provided colors are spaced out amongst
   * the ranges that exist.
   *
   * For example, if we are provided a list of 9 colors on a red
   * to green gradient, but only have three maximum values for 3 value ranges, 
   * we expect to get back a ColorBreaks object with three ranges and three colors, 
   * with the first being red, the second color being the 5th
   * color (between red and green), and the last being green.
   *
   * @param limits  An array of the maximum value of each range
   * @param colors  An array of RGBA color values
   */
  def assign(limits:Array[Int], colors:Array[Int]) = {
    if (limits.length != colors.length) {
      val used = new Array[Int](limits.length)
      used(0) = colors(0)
  
      val b = limits.length - 1
      val c = colors.length - 1
      var i = 1
      while (i < limits.length) {
        used(i) = colors(round(i.toDouble * c / b).toInt)
        i += 1
      }
  
      new ColorBreaks(limits, used)
    } else {
      new ColorBreaks(limits, colors)
    }
  }
}

/**
 * Color blender used for generating symbology for class breaks.
 */
object Blender {
  /**
    * Interpolate value for individual color band (0-255).  
    */
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
  // meant to be used with single numbers, not RGB values
  def getRanges(masker:(Int) => Int, num:Int):Array[Int]

  // Returns a sequence of RGBA integer values
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
 * Generates colors on a gradient between an initial color and an end color.
 *
 * Note that each band has its own gradient, including the alpha channel.
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
  def getRanges(masker:Int => Int, count:Int) = {
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

