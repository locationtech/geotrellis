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
import scala.collection.mutable

import geotrellis.raster.histogram.Histogram
import java.util.Locale

import scala.reflect.ClassTag

abstract class ColorClassifier[A: ClassTag] extends Serializable {
  type Classification = (Double, Int)
  protected var classificationBoundaries: mutable.Set[A] = mutable.Set[A]()
  protected var classificationColors: mutable.Buffer[RGBA] = mutable.ArrayBuffer[RGBA]()
  protected var noDataColor: Option[RGBA] = None

  lazy val length = classificationBoundaries.size

  def getBreaks: Array[A] = classificationBoundaries.toArray
  def getColors: Array[RGBA] = classificationColors.toArray
  def getNoDataColor = noDataColor

  def mapColors(f: RGBA => RGBA): ColorClassifier[A] = {
    classificationColors = classificationColors.map(f)
    noDataColor = noDataColor.map(f)
    this
  }

  def classify(classBoundary: A, classColor: RGBA): ColorClassifier[A] = {
    if (classificationBoundaries.add(classBoundary)) classificationColors.append(classColor)
    this  // For chaining multiple classifications together
  }

  def setNoDataColor(color: RGBA): ColorClassifier[A] = {
    noDataColor = Some(color)
    this
  }

  /**
    * If the count of colors doesn't match the count of classification boundaries, produce a
    * ColorClassification which either interpolates or properly subsets the colors so as
    * to have an equal count of boundaries and colors
  **/
  protected def normalize: ColorClassifier[A] = {
    if (classificationBoundaries.size < classificationColors.size) {
      classificationColors = spread(getColors, classificationBoundaries.size).toBuffer
    } else if (classificationBoundaries.size > classificationColors.size) {
      classificationColors = chooseColors(getColors, classificationBoundaries.size).toBuffer
    }
    this
  }

  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap

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
  protected def spread(colors: Array[RGBA], n: Int): Array[RGBA] = {
    if (colors.length == n) return colors

    val colors2 = new Array[RGBA](n)
    colors2(0) = colors(0)

    val b = n - 1
    val color = colors.length - 1
    var i = 1
    while (i < n) {
      colors2(i) = colors(math.round(i.toDouble * color / b).toInt)
      i += 1
    }

    colors2
  }

  // Interpolation logic
  protected def blend(start: Int, end: Int, numerator: Int, denominator: Int): Int = {
    start + (((end - start) * numerator) / denominator)
  }

  protected def chooseColors(c: Array[RGBA], numColors: Int): Array[RGBA] =
    getColorSequence(numColors) { (masker: RGBA => Int, count: Int) =>
      val hues = c.map(masker)
      val mult = c.length - 1
      val denom = count - 1

      if (count < 2) {
        Array(hues(0))
      } else {
        val ranges = new Array[Int](count)
        var i = 0
        while (i < count) {
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

  protected def chooseColors(color1: RGBA, color2: RGBA, numColors: Int): Array[RGBA] =
    getColorSequence(numColors) { (masker: RGBA => Int, count: Int) =>
      val start = masker(color1)
      val end   = masker(color2)
      if (numColors < 2) {
        Array(start)
      } else {
        val ranges = new Array[Int](numColors)
        var i = 0
        while (i < numColors) {
          ranges(i) = blend(start, end, i, numColors - 1)
          i += 1
        }
        ranges
      }
    }

  /** Returns a sequence of RGBA integer values */
  protected def getColorSequence(n: Int)(getRanges: (RGBA => Int, Int) => Array[Int]): Array[RGBA] = {
    val unzipR = { color: RGBA => color.red }
    val unzipG = { color: RGBA => color.green }
    val unzipB = { color: RGBA => color.blue }
    val unzipA = { color: RGBA => color.alpha }
    val rs = getRanges(unzipR, n)
    val gs = getRanges(unzipG, n)
    val bs = getRanges(unzipB, n)
    val as = getRanges(unzipA, n)

    val theColors = new Array[RGBA](n)
    var i = 0
    while (i < n) {
      theColors(i) = RGBA(rs(i), gs(i), bs(i), as(i))
      i += 1
    }
    theColors
  }
}

class IntColorClassifier extends ColorClassifier[Int] {
  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap = {
    normalize
    ColorMap(getBreaks, getColors.map(_.get), options)
  }
}

class DoubleColorClassifier extends ColorClassifier[Double] {
  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap = {
    normalize
    ColorMap(getBreaks, getColors.map(_.get), options)
  }
}

object ColorClassifier {
  def apply(classifications: Array[(Int, RGBA)]): IntColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Int, RGBA)], noDataColor: Option[RGBA]): IntColorClassifier = {
    val colorClassifier = new IntColorClassifier
    classifications foreach { case classification: (Int, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }

  def apply(classifications: Array[(Double, RGBA)]): DoubleColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Double, RGBA)], noDataColor: Option[RGBA]): DoubleColorClassifier = {
    val colorClassifier = new DoubleColorClassifier
    classifications foreach { case classification: (Double, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }
}
