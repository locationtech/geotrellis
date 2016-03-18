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

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram

import scala.collection.mutable
import scala.util.Try

/** Root element in hierarchy for specifying the type of boundary when classifying colors*/
sealed trait ClassBoundaryType
case object GreaterThan extends ClassBoundaryType
case object GreaterThanOrEqualTo extends ClassBoundaryType
case object LessThan extends ClassBoundaryType
case object LessThanOrEqualTo extends ClassBoundaryType
case object Exact extends ClassBoundaryType

object ColorMap {
  case class Options(
    classBoundaryType: ClassBoundaryType = LessThan,
    /** Rgba value for NODATA */
    noDataColor: Int = 0x00000000,
    /** Rgba value for data that doesn't fit the map */
    fallbackColor: Int = 0x00000000,
    /** Set to true to throw exception on unmappable variables */
    strict: Boolean = false
  )

  object Options {
    def DEFAULT = Options()

    implicit def classBoundaryTypeToOptions(classBoundaryType: ClassBoundaryType): Options =
      Options(classBoundaryType)
  }

  def apply(breaksToColors: (Int, Int)*): IntColorMap =
    apply(breaksToColors.toMap)

  def apply(breaksToColors: Map[Int, Int]): IntColorMap =
    apply(breaksToColors, Options.DEFAULT)

  def apply(breaksToColors: Map[Int, Int],
            options: Options): IntColorMap =
    new IntColorMap(breaksToColors, options)

  def apply(breaksToColors: (Double, Int)*): DoubleColorMap =
    apply(breaksToColors.toMap)

  def apply(breaksToColors: Map[Double, Int]): DoubleColorMap =
    apply(breaksToColors, Options.DEFAULT)

  def apply(breaksToColors: Map[Double, Int], options: Options): DoubleColorMap =
    new DoubleColorMap(breaksToColors, options)

  def apply(breaks: Array[Int], colorRamp: ColorRamp): IntColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Array[Int], colorRamp: ColorRamp, options: Options): IntColorMap =
    apply(breaks.toVector, colorRamp, options)

  def apply(breaks: Vector[Int], colorRamp: ColorRamp): IntColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Vector[Int], colorRamp: ColorRamp, options: Options): IntColorMap = {
    val breaksToColors: Map[Int, Int] = (breaks zip colorRamp.stops(breaks.size).colors).toMap
    apply(breaksToColors, options)
  }

  def apply(breaks: Array[Double], colorRamp: ColorRamp): DoubleColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Array[Double], colorRamp: ColorRamp, options: Options): DoubleColorMap =
    apply(breaks.toVector, colorRamp, options)

  def apply(breaks: Vector[Double], colorRamp: ColorRamp): DoubleColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Vector[Double], colorRamp: ColorRamp, options: Options): DoubleColorMap = {
    val breaksToColors: Map[Double, Int] =
      breaks.zip(colorRamp.stops(breaks.size).colors)
            .toMap

    apply(breaksToColors, options)
  }

  def fromQuantileBreaks(histogram: Histogram[Int], colorRamp: ColorRamp): IntColorMap =
    fromQuantileBreaks(histogram, colorRamp, Options.DEFAULT)

  def fromQuantileBreaks(histogram: Histogram[Int], colorRamp: ColorRamp, options: Options): IntColorMap =
    apply(histogram.quantileBreaks(colorRamp.numStops).toVector, colorRamp.colors, options)

  def fromQuantileBreaks(histogram: Histogram[Double], colorRamp: ColorRamp): DoubleColorMap =
    fromQuantileBreaks(histogram, colorRamp, Options.DEFAULT)

  def fromQuantileBreaks(histogram: Histogram[Double], colorRamp: ColorRamp, options: Options): DoubleColorMap =
    apply(histogram.quantileBreaks(colorRamp.numStops).toVector, colorRamp.colors, options)

  /** Parse an integer color map from a string of value:color;value:color;... */
  def fromString(str: String): Option[IntColorMap] = {
    val split = str.split(';').map(_.trim.split(':'))
    Try {
      val limits = split.map { pair => pair(0).toInt }
      val colors = split.map { pair => BigInt(pair(1), 16).toInt }
      require(limits.size == colors.size)
      apply(limits, colors)
    }.toOption
  }

  /** Parse an integer color map from a string of value:color;value:color;... */
  def fromStringDouble(str: String): Option[DoubleColorMap] = {
    val split = str.split(';').map(_.trim.split(':'))
    Try {
      val limits = split.map { pair => pair(0).toDouble }
      val colors = split.map { pair => BigInt(pair(1), 16).toInt }
      require(limits.size == colors.size)
      apply(limits, colors)
    }.toOption
  }

}

import ColorMap.Options

trait ColorMap extends Serializable {
  def colors: Vector[Int]
  val options: Options

  lazy val (opaque, grey) = {
    var opaque = true
    var grey = true
    var i = 0
    while (i < colors.length) {
      val c = colors(i)
      opaque &&= RGBA(c).isOpaque
      grey &&= RGBA(c).isGrey
      i += 1
    }
    (opaque, grey)
  }

  def render(r: Tile): Tile =
    if(r.cellType.isFloatingPoint) {
      r.convert(IntCellType).mapDouble { z => mapDouble(z).toInt }
    } else {
      r.convert(IntCellType).map(map _)
    }

  def map(v: Int): Int
  def mapDouble(v: Double): Int

  def mapColors(f: Int => Int): ColorMap
  def mapColorsToIndex(): ColorMap

  def withNoDataColor(color: Int): ColorMap
  def withFallbackColor(color: Int): ColorMap
  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap
}

class IntColorMap(breaksToColors: Map[Int, Int], val options: Options = Options.DEFAULT) extends ColorMap {
  private lazy val orderedBreaks: Vector[Int] =
    options.classBoundaryType match {
      case LessThan | LessThanOrEqualTo =>
        breaksToColors.keys.toVector.sorted
      case GreaterThan | GreaterThanOrEqualTo =>
        breaksToColors.keys.toVector.sorted.reverse
      case Exact =>
        breaksToColors.keys.toVector
    }

  private lazy val orderedColors: Vector[Int] = orderedBreaks.map(breaksToColors(_))
  lazy val colors = orderedColors

  private val zCheck: (Int, Int) => Boolean =
    options.classBoundaryType match {
      case LessThan =>
        { (z: Int, i: Int) => z > orderedBreaks(i) }
      case LessThanOrEqualTo =>
        { (z: Int, i: Int) => z >= orderedBreaks(i) }
      case GreaterThan =>
        { (z: Int, i: Int) => z < orderedBreaks(i) }
      case GreaterThanOrEqualTo =>
        { (z: Int, i: Int) => z <= orderedBreaks(i) }
      case Exact =>
        { (z: Int, i: Int) => z != orderedBreaks(i) }
    }

  private val len = orderedBreaks.length

  def map(z: Int) = {
    if(isNoData(z)) { options.noDataColor }
    else {
      var i = 0
      while(i < len && zCheck(z, i)) { i += 1 }
      if(i == len){
        if(options.strict) {
          sys.error(s"Value $z did not have an associated color and break")
        } else {
          options.fallbackColor
        }
      } else {
        orderedColors(i)
      }
    }
  }

  def mapDouble(z: Double) =
    map(d2i(z))

  def mapColors(f: Int => Int): ColorMap =
    new IntColorMap(breaksToColors.map { case (key, color) => (key, f(color)) }, options)

  def mapColorsToIndex(): ColorMap =
    new IntColorMap(orderedBreaks.zipWithIndex.toMap, options)

  def withNoDataColor(color: Int): ColorMap =
    new IntColorMap(breaksToColors, options.copy(noDataColor = color))

  def withFallbackColor(color: Int): ColorMap =
    new IntColorMap(breaksToColors, options.copy(fallbackColor = color))

  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap =
    new IntColorMap(breaksToColors, options.copy(classBoundaryType = classBoundaryType))

  def cache(h: Histogram[Int]): ColorMap = {
    val ch = h.mutable
    h.foreachValue(z => ch.setItem(z, map(z)))
    new IntCachedColorMap(orderedColors, ch, options)
  }
}

/** Caches a color ramp based on a histogram of values. This is an optimization, since
  * often times we create a histogram while classifying, and can reuse that computed
  * information in the color mapping.
  */
class IntCachedColorMap(val colors: Vector[Int], h: Histogram[Int], val options: Options)
    extends ColorMap {
  val noDataColor = options.noDataColor

  def map(z: Int): Int = { if(isNoData(z)) noDataColor else h.itemCount(z) }

  def mapDouble(z: Double): Int = map(d2i(z))

  def mapColors(f: Int => Int): ColorMap = {
    val ch = h.mutable
    h.foreachValue(z => ch.setItem(z, f(h.itemCount(z))))
    new IntCachedColorMap(colors, ch, options)
  }

  def mapColorsToIndex(): ColorMap = {
    val colorIndexMap = colors.zipWithIndex.map { case (c, i) => (i, c) }.toMap
    val ch = h.mutable
    h.foreachValue(z => ch.setItem(z, colorIndexMap(h.itemCount(z))))
    new IntCachedColorMap((0 to colors.length).toVector, ch, options)
  }

  def withNoDataColor(color: Int): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(noDataColor = color))

  def withFallbackColor(color: Int): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(fallbackColor = color))

  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(classBoundaryType = classBoundaryType))
}

class DoubleColorMap(breaksToColors: Map[Double, Int], val options: Options = Options.DEFAULT) extends ColorMap {

  private val orderedBreaks: Vector[Double] =
    options.classBoundaryType match {
      case LessThan | LessThanOrEqualTo =>
        breaksToColors.keys.toVector.sorted
      case GreaterThan | GreaterThanOrEqualTo =>
        breaksToColors.keys.toVector.sorted.reverse
      case Exact =>
        breaksToColors.keys.toVector
    }

  private val orderedColors: Vector[Int] = orderedBreaks.map(breaksToColors(_))
  lazy val colors = orderedColors

  private val zCheck: (Double, Int) => Boolean =
    options.classBoundaryType match {
      case LessThan =>
        { (z: Double, i: Int) => z > orderedBreaks(i) }
      case LessThanOrEqualTo =>
        { (z: Double, i: Int) => z >= orderedBreaks(i) }
      case GreaterThan =>
        { (z: Double, i: Int) => z < orderedBreaks(i) }
      case GreaterThanOrEqualTo =>
        { (z: Double, i: Int) => z <= orderedBreaks(i) }
      case Exact =>
        { (z: Double, i: Int) => z != orderedBreaks(i) }
    }

  private val len = orderedBreaks.length

  def map(z: Int) = { mapDouble(i2d(z)) }

  def mapDouble(z: Double) = {
    if(isNoData(z)) { options.noDataColor }
    else {
      var i = 0
      while(i < len && zCheck(z, i)) { i += 1 }

      if(i == len){
        if(options.strict) {
          sys.error(s"Value $z did not have an associated color and break")
        } else {
          options.fallbackColor
        }
      } else {
        breaksToColors(orderedBreaks(i))
      }
    }
  }

  def mapColors(f: Int => Int): ColorMap =
    new DoubleColorMap(breaksToColors.map { case (key, color) => (key, f(color)) }, options)

  def mapColorsToIndex(): ColorMap =
    new DoubleColorMap(orderedBreaks.zipWithIndex.toMap, options)

  def withNoDataColor(color: Int): ColorMap =
    new DoubleColorMap(breaksToColors, options.copy(noDataColor = color))

  def withFallbackColor(color: Int): ColorMap =
    new DoubleColorMap(breaksToColors, options.copy(fallbackColor = color))

  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap =
    new DoubleColorMap(breaksToColors, options.copy(classBoundaryType = classBoundaryType))
}
