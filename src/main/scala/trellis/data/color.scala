package trellis.data

import scala.math.{round,min,max}
import java.nio.{ByteBuffer}
import Console.printf

/**
  * Data object to represent class breaks, stored as an array of ranges.
  * Each individual range is stored as a tuple of (min value, max value)
  */
case class ColorBreaks(val breaks:Array[(Int, Int)])

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
  /* */
  def getColors(num:Int):Seq[Int]

  /* helper stuff */
  def unzipRGB(color:Int) = {
    val r = (color >> 16) & 0xff
    val g = (color >> 8) & 0xff
    val b = (color) & 0xff
    (r, g, b)
  }

  def zipRGB(r:Int, g:Int, b:Int) = { (r<<16) + (g<<8) + b }

  // returns a string of hex colors: "ff0000,00ff00,0000ff"
  def getColorString(num:Int) = {
    val colors = this.getColors(num)
    colors.map("%06X".format(_)).reduceLeft(_ + "," + _)
  }
}

/**
  * Abstract class for generating a range of colors. 
  */
abstract class ColorRangeChooser extends ColorChooser {
  // meant to be used with single numbesr, not RGB values
  def getRanges(masker:(Int) => Int, num:Int):Seq[Int]

  // returns a sequence of integers
  def getColors(num:Int):Seq[Int] = {
    val rs = getRanges((x) => (x >> 16) & 0xff, num)
    val gs = getRanges((x) => (x >> 8) & 0xff, num)
    val bs = getRanges((x) => (x) & 0xff, num)

    (0 until num).map { i => zipRGB(rs(i), gs(i), bs(i)) }
  }

}

/**
  * Generates colors between an initial color and an end color.
  */
case class LinearColorRangeChooser(val color1:Int,
                              val color2:Int) extends ColorRangeChooser {
  def getRanges(masker:(Int) => Int, num:Int) = {
    val start = masker(this.color1)
    val end   = masker(this.color2)
    if (num < 2) {
      List(start)
    } else {
      (0 until num).map { i => Blender.blend(start, end, i, num - 1) }.toList
    }
  }

}


/**
  * Generates a range of colors from an array of initial colors. 
  */
case class MultiColorRangeChooser(val colors:Array[Int]) extends ColorRangeChooser {
  def getRanges(masker:(Int) => Int, count:Int) = {
    val hues  = this.colors.map(masker)
    val n     = this.colors.length - 1
    val denom = count - 1

    (0 until count).map {
      i => {
        if (denom < 1) {
          hues(0)
        } else {
          val j = (i * n) / denom
          if (j < n) {
            val num = (i * n) % denom
            val x = Blender.blend(hues(j), hues(j + 1), num, denom)
            //printf("%d: hue-%d(%02x) and hue-%d(%02x) (%d/%d) -> %02x\n", i, j, hues(j), j + 1, hues(j + 1), k, denom, x)
            x
          } else {
            //printf("%d: hue-%d(%02x)\n", i, j, hues(j))
            hues(j)
          }
        }
      }
    }.toList
  }
}
