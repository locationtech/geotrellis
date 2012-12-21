package geotrellis.data

import scala.math.round

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
  def apply(z:Int):Int = if (z == NODATA) nodataColor else cb.get(z)
}

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
