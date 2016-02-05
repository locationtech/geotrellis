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

class RasterColorClassifier[A: ClassTag, C <: Color: ClassTag] extends Serializable {
  type Classification = (A, Int)
  private val classificationBoundaries = mutable.ArrayBuffer[A]()
  private val classificationColors = mutable.ArrayBuffer[C]()
  private var noDataColor: Option[Int] = None

  def classify(classBoundary: A, classColor: C): RasterColorClassifier[A, C] = {
    classificationBoundary.append(classBoundary)
    classificationColors.append(classColor.x)
    this  // For chaining multiple classifications together
  }

  def setNoDataColor(color: C): RasterColorClassifier[A, C] = {
    noDataColor = Some(color)
    this
  }

  lazy val length = classificationBoundaries.size

  def mapColors(f: Int => Int): RasterColorClassifier[A, C] =
    RasterColorClassifier(bounds zip colors.map(f), noDataColor.map(f))

  def toColorMap(options: ColorMapOptions = ColorMapOptions.Default): ColorMap =
    ColorMap(classifications._1, classifications._2, options)

  def getNoDataColor = noDataColor

  def bounds: Array[A] = classificationBoundaries.toArray

  def colors: Array[C] = classificationColors.toArray

  /**
    * If the count of colors doesn't match the count of classification boundaries, produce a
    * RasterColorClassification which either interpolates or properly subsets the colors so as
    * to have an equal count of boundaries and colors
  **/
  def normalize: RasterColorClassifier[A, C] = {
    if (classificationBoundaries.size < classificationColors.size) {
      val newColors = spread(colors, classificationBoundaries.size)
      RasterColorClassifier(bounds zip newColors, noDataColor)
    } else if (classificationBoundaries.size > classificationColors.size) {
      val newColors = chooseColors(colors, classificationBoundaries.size)
      RasterColorClassifier(bounds zip newColors, noDataColor)
    } else {
      this
    }
  }

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
  def spread(c: Array[C], n: Int): Array[C] = {
    if (c.length == n) return colors

    val colors2 = new Array[C](n)
    colors2(0) = c(0)

    val b = n - 1
    val c = c.length - 1
    var i = 1
    while (i < n) {
      colors2(i) = c(math.round(i.toDouble * c / b).toInt)
      i += 1
    }

    colors2
  }

  // Interpolation logic
  def blend(start: C, end: C, numerator: Int, denominator: Int): C = {
    start + (((end - start) * numerator) / denominator)
  }

  def chooseColors(c: Array[C], numColors: Int): Array[C] =
    getColorSequence(numColors) { (masker: C => Int, count: Int) =>
      val hues = c.map(masker)
      val mult = c.length - 1
      val denom = count - 1

      if (count < 2) {
        Array(hues(0))
      } else {
        val ranges = new Array[C](count)
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

  def chooseColors(color1: C, color2: C, numColors: Int): Array[C] =
    getColorSequence(numColors) { (masker: C => Int, count: Int) =>
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
  def getColorSequence(n: Int)(getRanges: (C => Int, Int) => Array[Int]): Array[Int] = {
    val unzipR = { color: C => color.red }
    val unzipG = { color: C => color.green }
    val unzipB = { color: C => color.blue }
    val unzipA = { color: C => color.alpha }
    val rs = getRanges(unzipR, n)
    val gs = getRanges(unzipG, n)
    val bs = getRanges(unzipB, n)
    val as = getRanges(unzipA, n)

    val theColors = new Array[Int](n)
    var i = 0
    while (i < n) {
      theColors(i) = RGBA(rs(i), gs(i), bs(i), as(i)).x
      i += 1
    }
    theColors
  }
}


object RasterColorClassifier {
  def apply[A: ClassTag, C <: Color: ClassTag](classifications: Array[(A, C)], noDataColor: Option[Int] = None): RasterColorClassifier[A, C] = {
    val colorClassifier = new RasterColorClassifier[A, C]
    classifications map { case classification: (A, C) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }
}
