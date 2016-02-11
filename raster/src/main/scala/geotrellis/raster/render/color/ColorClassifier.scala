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

sealed trait ClassBoundaryType

case object GreaterThan extends ClassBoundaryType
case object GreaterThanOrEqualTo extends ClassBoundaryType
case object LessThan extends ClassBoundaryType
case object LessThanOrEqualTo extends ClassBoundaryType
case object Exact extends ClassBoundaryType


sealed abstract class ColorClassifier[A] extends Serializable {
  protected var noDataColor: RGBA = RGBA(0x00000000)
  protected var fallbackColor: RGBA = RGBA(0x00000000)

  val classificationType: ClassBoundaryType

  def getBreaks: Array[A]
  def getColors: Array[RGBA]

  def mapBreaks(f: A => A): ColorClassifier[A]
  def mapColors(f: RGBA => RGBA): ColorClassifier[A]

  def toColorMap(options: ColorMapOptions, histogram: Option[Histogram]): ColorMap

  def length: Int

  def getNoDataColor = noDataColor

  def classify(classBreak: A, classColor: RGBA): ColorClassifier[A]

  def addClassifications(classifications: Array[(A, RGBA)]): ColorClassifier[A] =
    addClassifications(classifications: _*)

  def addClassifications(classifications: (A, RGBA)*): ColorClassifier[A] = {
    classifications map { case classification: (A, RGBA) =>
      classify(classification._1, classification._2)
    }
    this
  }

  def setNoDataColor(color: RGBA): ColorClassifier[A] = {
    noDataColor = color
    this
  }

  def setFallbackColor(color: RGBA): ColorClassifier[A] = {
    fallbackColor = color
    this
  }

  protected def cmapOptions: ColorMapOptions =
    ColorMapOptions(classificationType, noDataColor.get, fallbackColor.get, false)

}

trait StrictColorClassifier[A] { this: ColorClassifier[A] =>
  protected var colorClassifications: mutable.Map[A, RGBA] = mutable.Map[A, RGBA]()
  implicit val ctag: ClassTag[A]
  implicit val ord: Ordering[A]

  def length = colorClassifications.size

  def getBreaks: Array[A] = {
    colorClassifications.keys.toArray.sorted
  }

  def getColors: Array[RGBA] =
    getBreaks.map { break =>
      colorClassifications(break)
    }

  def mapBreaks(f: A => A): ColorClassifier[A] = {
    val newMap = mutable.Map[A, RGBA]()
    colorClassifications map { case (k, v) =>
      newMap(f(k)) = v
    }
    colorClassifications = newMap
    this
  }

  def mapColors(f: RGBA => RGBA): ColorClassifier[A] = {
    colorClassifications map { case (k, v) =>
      colorClassifications(k) = v
    }
    this
  }

  def classify(classBreak: A, classColor: RGBA): ColorClassifier[A] = {
    colorClassifications(classBreak) = classColor
    this
  }
}

trait BlendingColorClassifier[A] { this: ColorClassifier[A] =>
  protected var classificationBreaks: mutable.Buffer[A] = mutable.Buffer[A]()
  protected var classificationColors: mutable.Buffer[RGBA] = mutable.ArrayBuffer[RGBA]()
  implicit val ctag: ClassTag[A]
  implicit val ord: Ordering[A]

  def length: Int = classificationBreaks.size

  def getBreaks: Array[A] = classificationBreaks.toArray

  def getColors: Array[RGBA] = classificationColors.toArray

  def classify(classBreak: A, classColor: RGBA): ColorClassifier[A] = {
    addBreaks(classBreak)
    addColors(classColor)
    this
  }

  def mapBreaks(f: A => A): ColorClassifier[A] = {
    classificationBreaks = classificationBreaks map(f(_))
    this
  }

  def mapColors(f: RGBA => RGBA): ColorClassifier[A] = {
    classificationColors = classificationColors map(f(_))
    this
  }

  def addBreaks(breaks: Array[A]): BlendingColorClassifier[A] =
    addBreaks(breaks: _*)

  def addBreaks(breaks: A*): BlendingColorClassifier[A] = {
    classificationBreaks ++= breaks
    this
  }

  def addColors(colors: Array[RGBA]): BlendingColorClassifier[A] =
    addColors(colors: _*)

  def addColors(colors: RGBA*): BlendingColorClassifier[A] = {
    classificationColors ++= colors
    this
  }

  /**
    * If the count of colors doesn't match the count of classification Breaks, produce a
    * ColorClassification which either interpolates or properly subsets the colors so as
    * to have an equal count of Breaks and colors
  **/
  def normalize: BlendingColorClassifier[A] = {
    if (classificationBreaks.size < classificationColors.size) {
      classificationColors = spread(getColors, classificationBreaks.size).toBuffer
    } else if (classificationBreaks.size > classificationColors.size) {
      classificationColors = chooseColors(getColors, classificationBreaks.size).toBuffer
    }
    this
  }

  /**
  **/
  def alphaGradient(start: RGBA = RGBA(0), stop: RGBA = RGBA(0xFF)): BlendingColorClassifier[A] = {
    val colors = getColors
    val alphas = chooseColors(start, stop, colors.length).map(_.alpha)

    val newColors = colors.zip(alphas).map ({ case (color, a) =>
      val (r, g, b) = color.unzipRGB
      RGBA(r, g, b, a)
    })

    classificationColors = newColors.toBuffer
    this
  }

  def setAlpha(a: Int): BlendingColorClassifier[A] = {
    val newColors = getColors.map { color =>
      val(r, g, b) = color.unzipRGB
      RGBA(r, g, b, a)
    }
    classificationColors = newColors.toBuffer
    this
  }

  def setAlpha(alphaPct: Double): BlendingColorClassifier[A] = {
    val newColors: Array[RGBA] = getColors.map { color =>
      val(r, g, b) = color.unzipRGB
      RGBA(r, g, b, alphaPct)
    }
    classificationColors = newColors.toBuffer
    this
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


case class StrictIntColorClassifier(classificationType: ClassBoundaryType = LessThan)(implicit val ctag: ClassTag[Int], implicit val ord: Ordering[Int])
    extends ColorClassifier[Int]
       with StrictColorClassifier[Int] {

  def toColorMap(options: ColorMapOptions, histogram: Option[Histogram] = None): ColorMap = {
    histogram match {
      case Some(h) => ColorMap(getBreaks, getColors.map(_.get), cmapOptions).cache(h)
      case None =>  ColorMap(getBreaks, getColors.map(_.get), cmapOptions)
    }
  }
}

case class StrictDoubleColorClassifier(classificationType: ClassBoundaryType = LessThan)(implicit val ctag: ClassTag[Double], implicit val ord: Ordering[Double])
    extends ColorClassifier[Double]
       with StrictColorClassifier[Double] {

  def toColorMap(options: ColorMapOptions, histogram: Option[Histogram] = None): ColorMap = {
    histogram match {
      case Some(h) => ColorMap(getBreaks, getColors.map(_.get), cmapOptions).cache(h)
      case None =>  ColorMap(getBreaks, getColors.map(_.get), cmapOptions)
    }
  }
}

case class BlendingIntColorClassifier(classificationType: ClassBoundaryType = LessThan)(implicit val ctag: ClassTag[Int], implicit val ord: Ordering[Int])
    extends ColorClassifier[Int]
       with BlendingColorClassifier[Int] {

  def toColorMap(options: ColorMapOptions, histogram: Option[Histogram] = None): ColorMap = {
    normalize
    histogram match {
      case Some(h) => ColorMap(getBreaks, getColors.map(_.get), cmapOptions).cache(h)
      case None =>  ColorMap(getBreaks, getColors.map(_.get), cmapOptions)
    }
  }
}

case class BlendingDoubleColorClassifier(classificationType: ClassBoundaryType = LessThan)(implicit val ctag: ClassTag[Double], implicit val ord: Ordering[Double])
    extends ColorClassifier[Double]
       with BlendingColorClassifier[Double] {

  def toColorMap(options: ColorMapOptions, histogram: Option[Histogram] = None): ColorMap = {
    normalize
    histogram match {
      case Some(h) => ColorMap(getBreaks, getColors.map(_.get), cmapOptions).cache(h)
      case None =>  ColorMap(getBreaks, getColors.map(_.get), cmapOptions)
    }
  }
}

object StrictColorClassifier {
  def apply(classifications: Array[(Int, RGBA)]): StrictIntColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Int, RGBA)], noDataColor: Option[RGBA]): StrictIntColorClassifier = {
    val colorClassifier = new StrictIntColorClassifier
    classifications foreach { case classification: (Int, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }

  def withQuantileBreaks(histogram: Histogram, colors: Array[RGBA]) = {
    val breaks = histogram.getQuantileBreaks(colors.length)
    apply(breaks zip colors)
  }

  def apply(classifications: Array[(Double, RGBA)]): StrictDoubleColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Double, RGBA)], noDataColor: Option[RGBA]): StrictDoubleColorClassifier = {
    val colorClassifier = new StrictDoubleColorClassifier
    classifications foreach { case classification: (Double, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }
}

object BlendingColorClassifier {
  def apply(classifications: Array[(Int, RGBA)]): BlendingIntColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Int, RGBA)], noDataColor: Option[RGBA]): BlendingIntColorClassifier = {
    val colorClassifier = new BlendingIntColorClassifier
    classifications foreach { case classification: (Int, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }

  def apply(classifications: Array[(Double, RGBA)]): BlendingDoubleColorClassifier =
    apply(classifications, None)

  def apply(classifications: Array[(Double, RGBA)], noDataColor: Option[RGBA]): BlendingDoubleColorClassifier = {
    val colorClassifier = new BlendingDoubleColorClassifier
    classifications foreach { case classification: (Double, RGBA) =>
      colorClassifier.classify(classification._1, classification._2)
    }
    colorClassifier
  }
}
