/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.render

import scala.util.Try

import geotrellis.raster.histogram.Histogram
import java.util.Locale

/**
  * ColorBreaks describes a way to render a raster into a colored image.
  */
trait ColorBreaks extends Serializable {
  def ndColor: Option[Int]
  def colors: Array[Int]
  def length: Int
  def replaceColors(newColors: Array[Int]): ColorBreaks
  def mapColors(f: Int => Int): ColorBreaks
  def toColorMap(): ColorMap = toColorMap(ColorMapOptions.Default)
  def toColorMap(options: ColorMapOptions): ColorMap
}

/**
  * IntColorBreaks describes a way to render a raster into a colored image.
  *
  * This class defines a set of Int value ranges and assigns a color to
  * each value range.
  *
  * @param limits  An array with the maximum value of each range
  * @param colors  An array with the color assigned to each range
  */
class IntColorBreaks(val limits: Array[Int], val colors: Array[Int], val ndColor: Option[Int] = None) extends ColorBreaks {
  assert(limits.length == colors.length)
  assert(colors.length > 0)

  def length = limits.length

  def replaceColors(newColors: Array[Int]): ColorBreaks =
    new IntColorBreaks(limits, newColors)

  def mapColors(f: Int => Int): ColorBreaks =
    new IntColorBreaks(limits, colors.map(f))

  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap =
    ColorMap(limits, colors, options)

  override def toString = {
    val limitsStr = limits.mkString("Array(", ", ", ")")
    val colorsStr =
      colors
        .map("%08x" formatLocal(Locale.ENGLISH, _))
        .mkString("Array(", ", ", ")")
    s"IntColorBreaks($limitsStr, $colorsStr)"
  }
}

/**
  * DoubleColorBreaks describes a way to render a raster into a colored image.
  *
  * This class defines a set of Double value ranges and assigns a color to
  * each value range.
  *
  * @param limits  An array with the maximum value of each range
  * @param colors  An array with the color assigned to each range
  */
class DoubleColorBreaks(val limits: Array[Double], val colors: Array[Int], val ndColor: Option[Int] = None) extends ColorBreaks {
  assert(limits.length == colors.length)
  assert(colors.length > 0)

  def length = limits.length

  def replaceColors(newColors: Array[Int]): ColorBreaks =
    new DoubleColorBreaks(limits, newColors)

  def mapColors(f: Int => Int): ColorBreaks =
    new DoubleColorBreaks(limits, colors.map(f))

  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap =
    ColorMap(limits, colors, options)

  override def toString = {
    val limitsStr = limits.mkString("Array(", ", ", ")")
    val colorsStr =
      colors
        .map("%08x" formatLocal(Locale.ENGLISH, _))
        .mkString("Array(", ", ", ")")
    s"DoubleColorBreaks($limitsStr, $colorsStr)"
  }
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
  private def sample(colors: Array[Int], numSamples: Int) = {
    if (numSamples != colors.length) {
      val used = new Array[Int](numSamples)
      used(0) = colors(0)

      val b = numSamples - 1
      val c = colors.length - 1
      var i = 1
      while (i < numSamples) {
        used(i) = colors(math.round(i.toDouble * c / b).toInt)
        i += 1
      }

      used
    } else {
      colors
    }
  }

  def apply(limits: Array[Int], colors: Array[Int]): IntColorBreaks =
    apply(limits, colors, None)

  def apply(limits: Array[Int], colors: Array[Int], ndColor: Option[Int]): IntColorBreaks =
    new IntColorBreaks(limits, sample(colors, limits.length))

  def apply(limits: Array[Double], colors: Array[Int]): DoubleColorBreaks =
    apply(limits, colors, None)

  def apply(limits: Array[Double], colors: Array[Int], ndColor: Option[Int]): DoubleColorBreaks =
    new DoubleColorBreaks(limits, sample(colors, limits.length))

  def apply(histogram: Histogram, colors: Array[Int]): IntColorBreaks =
    apply(histogram, colors, None)

  def apply(histogram: Histogram, colors: Array[Int], ndColor: Option[Int]): IntColorBreaks = {
    val limits = histogram.getQuantileBreaks(colors.length)
    new IntColorBreaks(limits, sample(colors, limits.length))
  }

  /**
   * A utility method for getting the NoData color from a serialization of a ColorBreaks instance.
   * This method will grab the first 'null' field and attempt to make it work; falling back
   * to none in case that value doesn't work.
   */
  private def extractNoDataColor(breaks: String): Option[Int] = {
    val nulls = breaks.split(';').filter(_.contains("null"))
    nulls.headOption.map { colorString =>
      val hexColor = colorString.split(':')(1)
      BigInt(hexColor, 16).toInt
    }
  }

  /**
    * Convert from a string of the form: `<BREAK>:<COLOR>;<BREAK>:<COLOR>' to Option[DoubleColorBreaks]
   */
  def fromStringDouble(breaks: String): Option[DoubleColorBreaks] = {
    val split = breaks.split(';').map(_.trim.split(':'))
    Try {
      val limits = split.map { pair => pair(0).toDouble }
      val colors = split.map { pair => BigInt(pair(1), 16).toInt }
      require(limits.size == colors.size)
      // Handle possible NoData coloring
      val ndColor: Option[Int] = extractNoDataColor(breaks)
      ColorBreaks(limits, colors, ndColor)
    }.toOption
  }

  /**
    * Convert from a string of the form: `<BREAK>:<COLOR>;<BREAK>:<COLOR>' to Option[IntColorBreaks]
   */
  def fromStringInt(breaks: String): Option[IntColorBreaks] = {
    val split = breaks.split(';').map(_.trim.split(':'))
    Try {
      val limits = split.map { pair => pair(0).toInt }
      val colors = split.map { pair => BigInt(pair(1), 16).toInt }
      require(limits.size == colors.size)
      // Handle possible NoData coloring
      val ndColor: Option[Int] = extractNoDataColor(breaks)
      ColorBreaks(limits, colors, ndColor)
    }.toOption
  }
}
