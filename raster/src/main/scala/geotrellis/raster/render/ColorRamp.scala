/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.render

import geotrellis.raster.histogram._

/** A ColorRamp represents a sequence of RGBA color values */
class ColorRamp(val colors: Vector[Int]) extends Serializable {
  def numStops: Int = colors.length

  def stops(requestedNumStops: Int): ColorRamp =
    if(requestedNumStops < numStops)
      ColorRamp(ColorRamp.spread(colors, requestedNumStops))
    else if(requestedNumStops > numStops)
      ColorRamp(ColorRamp.chooseColors(colors, requestedNumStops))
    else
      this

  /** Transform this instances colors such that the transformed colors sit along an alpha gradient.
    *
    * @param start An integer whose last two bytes are the alpha value at the start of the
    *              gradient (default 0)
    * @param stop  An integer whose last two bytes are the alpha value at the end of the
    *              gradient (default 255)
    */
  def setAlphaGradient(start: Int = 0, stop: Int = 0xFF): ColorRamp = {
    val alphas = ColorRamp.chooseColors(Vector(start, stop), colors.length).map(_.alpha)

    val newColors =
      colors
        .zip(alphas)
        .map { case (color, a) =>
          val (r, g, b) = color.unzipRGB
          RGBA(r, g, b, a).int
        }

    ColorRamp(newColors)
  }

  /** Set a uniform alpha value for all contained colors */
  def setAlpha(a: Int): ColorRamp = {
    val newColors =
      colors
        .map { color =>
          val (r, g, b) = color.unzipRGB
          RGBA(r, g, b, a).int
        }

    ColorRamp(newColors)
  }

  /** Set a uniform alpha value for all contained colors */
  def setAlpha(alphaPct: Double): ColorRamp = {
    val newColors =
      colors
        .map { color =>
          val (r, g, b) = color.unzipRGB
          RGBA(r, g, b, alphaPct).int
        }

    ColorRamp(newColors)
  }

  /** Overloads for creating a ColorMap from a color ramp, for convenience.
    * Try to keep parity with ColorMap object creations
    */

  def toColorMap(breaks: Array[Int]): ColorMap =
    toColorMap(breaks, ColorMap.Options.DEFAULT)

  def toColorMap(breaks: Array[Int], options: ColorMap.Options): ColorMap =
    ColorMap(breaks.toVector, this, options)

  def toColorMap(breaks: Vector[Int]): ColorMap =
    toColorMap(breaks, ColorMap.Options.DEFAULT)

  def toColorMap(breaks: Vector[Int], options: ColorMap.Options): ColorMap =
    ColorMap(breaks.toVector, this, options)

  def toColorMap(breaks: Array[Double])(implicit d: DummyImplicit): ColorMap =
    toColorMap(breaks, ColorMap.Options.DEFAULT)

  def toColorMap(breaks: Array[Double], options: ColorMap.Options)(implicit d: DummyImplicit): ColorMap =
    ColorMap(breaks.toVector, this, options)

  def toColorMap(breaks: Vector[Double])(implicit d: DummyImplicit): ColorMap =
    toColorMap(breaks, ColorMap.Options.DEFAULT)

  def toColorMap(breaks: Vector[Double], options: ColorMap.Options)(implicit d: DummyImplicit): ColorMap =
    ColorMap(breaks.toVector, this, options)

  def toColorMap(histogram: Histogram[Int]): ColorMap =
    toColorMap(histogram, ColorMap.Options.DEFAULT)

  def toColorMap(histogram: Histogram[Int], options: ColorMap.Options): ColorMap =
    ColorMap.fromQuantileBreaks(histogram, this, options)

  def toColorMap(histogram: Histogram[Double])(implicit d: DummyImplicit): ColorMap =
    toColorMap(histogram, ColorMap.Options.DEFAULT)

  def toColorMap(histogram: Histogram[Double], options: ColorMap.Options)(implicit d: DummyImplicit): ColorMap =
    ColorMap.fromQuantileBreaks(histogram, this, options)
}

object ColorRamp {
  implicit def intArrayToColorRamp(colors: Array[Int]): ColorRamp = apply(colors)
  implicit def intVectorToColorRamp(colors: Vector[Int]): ColorRamp = apply(colors)
  implicit def colorRampToIntArray(colorRamp: ColorRamp): Array[Int] = colorRamp.colors.toArray
  implicit def colorRampToIntVector(colorRamp: ColorRamp): Vector[Int] = colorRamp.colors

  def apply(colors: Array[Int]): ColorRamp = apply(colors.toVector)

  def apply(colors: Traversable[Int]): ColorRamp = new ColorRamp(colors.toVector)

  def apply(colors: Int*)(implicit d: DummyImplicit): ColorRamp = apply(colors)

  /**
    * This method will return a smaller list of colors the provided list of colors, spaced
    * out amongst the provided color list. Used internally for normalization
    *
    * For example, if we are provided a list of 9 colors on a red
    * to green gradient, but only need a list of 3, we expect to get back a
    * list of 3 colors with the first being red, the second color being the 5th
    * color (between red and green), and the last being green.
    *
    * @param colors  Provided RGBA color values
    * @param n       Length of list to return
    */
  def spread(colors: Vector[Int], n: Int): Vector[Int] = {
    /* The requested spread matches the original */
    if (colors.length == n) return colors

    /* In the case of a Layer comprised entirely of NODATA, a call to
     * `classBreaksDouble` will yield an empty `Array`. That propagates here,
     * where an attempt to index below results in a bounds exception.
     *
     * Guarding on this condition and returning an empty `Vector` instead
     * is still valid; a `ColorMap` that contains an empty `colors` field
     * will still perform as expected and refuse to colour NODATA locations,
     * thanks to the `noDataColor` field in `ColorMap.Options`.
     */
    if (n < 1) return Vector.empty[Int]

    val colors2 = new Array[Int](n)
    colors2(0) = colors(0)

    val b = n - 1
    val color = colors.length - 1
    var i = 1
    while (i < n) {
      colors2(i) = colors(math.round(i.toDouble * color / b).toInt)
      i += 1
    }
    colors2.toVector
  }

  /** RGBA interpolation logic */

  def chooseColors(c: Vector[Int], numColors: Int): Vector[Int] = c match {
    case c if c.isEmpty => c
    case _ => {
      getColorSequence(numColors) { (masker: Int => Int) =>
        val hues = c.map(masker)
        val mult = c.length - 1
        val denom = numColors - 1

        if (numColors < 2) {
          Array(hues(0))
        } else {
          val ranges = new Array[Int](numColors)
          var i = 0
          while (i < numColors) {
            val j = (i * mult) / denom
            ranges(i) = if (j < mult) {
              blend(hues(j), hues(j + 1), (i * mult) % denom, denom)
            } else {
              hues(j)
            }
            i += 1
          }
          ranges
        }
      }
    }
  }

  private def blend(start: Int, end: Int, numerator: Int, denominator: Int): Int = {
    start + (((end - start) * numerator) / denominator)
  }

  /** Returns a sequence of RGBA integer values */
  private def getColorSequence(n: Int)(getRanges: (Int => Int) => Array[Int]): Vector[Int] = n match {
    case n if n < 1 => Vector.empty[Int]
    case _ => {
      val unzipR = { color: Int => color.red }
      val unzipG = { color: Int => color.green }
      val unzipB = { color: Int => color.blue }
      val unzipA = { color: Int => color.alpha }
      val rs = getRanges(unzipR)
      val gs = getRanges(unzipG)
      val bs = getRanges(unzipB)
      val as = getRanges(unzipA)

      val theColors = new Array[Int](n)
      var i = 0
      while (i < n) {
        theColors(i) = RGBA(rs(i), gs(i), bs(i), as(i))
        i += 1
      }
      theColors.toVector
    }
  }
}
