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

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.util._
import spire.syntax.cfor._
import spire.std.any._

import scala.util.Try

object ColorMap {
  case class Options(
    classBoundaryType: ClassBoundaryType = LessThanOrEqualTo,
    /** Rgba value for NODATA */
    noDataColor: Int = 0x00000000,
    /** Rgba value for data that doesn't fit the map */
    fallbackColor: Int = 0x00000000,
    /** Set to true to throw exception on unmappable variables */
    strict: Boolean = false
  ) {
    /** Conversion to a [[MapStrategy]] to be used by a [[BreakMap]]. */
    def strategy: MapStrategy[Int] =
      new MapStrategy(classBoundaryType, noDataColor, fallbackColor, strict)
  }

  object Options {
    def DEFAULT: Options = Options()

    implicit def classBoundaryTypeToOptions(cbt: ClassBoundaryType): Options =
      Options(cbt)
  }

  def apply(breaksToColors: (Int, Int)*): ColorMap =
    apply(breaksToColors.toMap)

  def apply(breaksToColors: Map[Int, Int]): ColorMap =
    apply(breaksToColors, Options.DEFAULT)

  def apply(breaksToColors: Map[Int, Int], options: Options): ColorMap =
    new IntColorMap(breaksToColors, options)

  def apply(breaksToColors: (Double, Int)*)(implicit d: DummyImplicit): ColorMap =
    apply(breaksToColors.toMap)

  def apply(breaksToColors: Map[Double, Int])(implicit d: DummyImplicit): ColorMap =
    apply(breaksToColors, Options.DEFAULT)

  def apply(breaksToColors: Map[Double, Int], options: Options)(implicit d: DummyImplicit): ColorMap =
    new DoubleColorMap(breaksToColors, options)

  def apply(breaks: Array[Int], colorRamp: ColorRamp): ColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Array[Int], colorRamp: ColorRamp, options: Options): ColorMap =
    apply(breaks.toVector, colorRamp, options)

  def apply(breaks: Vector[Int], colorRamp: ColorRamp): ColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Vector[Int], colorRamp: ColorRamp, options: Options): ColorMap = {
    val breaksToColors: Map[Int, Int] = (breaks zip colorRamp.stops(breaks.size).colors).toMap
    apply(breaksToColors, options)
  }

  def apply(breaks: Array[Double], colorRamp: ColorRamp)(implicit d: DummyImplicit): ColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Array[Double], colorRamp: ColorRamp, options: Options)(implicit d: DummyImplicit): ColorMap =
    apply(breaks.toVector, colorRamp, options)

  def apply(breaks: Vector[Double], colorRamp: ColorRamp)(implicit d: DummyImplicit): ColorMap =
    apply(breaks, colorRamp, Options.DEFAULT)

  def apply(breaks: Vector[Double], colorRamp: ColorRamp, options: Options)(implicit d: DummyImplicit): ColorMap = {
    val breaksToColors: Map[Double, Int] =
      breaks.zip(colorRamp.stops(breaks.size).colors)
            .toMap

    apply(breaksToColors, options)
  }

  def fromQuantileBreaks(histogram: Histogram[Int], colorRamp: ColorRamp): ColorMap =
    fromQuantileBreaks(histogram, colorRamp, Options.DEFAULT)

  def fromQuantileBreaks(histogram: Histogram[Int], colorRamp: ColorRamp, options: Options): ColorMap =
    apply(histogram.quantileBreaks(colorRamp.numStops).toVector, colorRamp.colors, options)

  def fromQuantileBreaks(histogram: Histogram[Double], colorRamp: ColorRamp)(implicit d: DummyImplicit): ColorMap =
    fromQuantileBreaks(histogram, colorRamp, Options.DEFAULT)

  def fromQuantileBreaks(histogram: Histogram[Double], colorRamp: ColorRamp, options: Options)(implicit d: DummyImplicit): ColorMap = {
    // Since double histograms are approximate,
    // set the fallback color to the appropriate color to catch
    // values beyond the limis
    val cm = apply(histogram.quantileBreaks(colorRamp.numStops).toVector, colorRamp.colors, options)
    options.classBoundaryType match {
      case GreaterThan | GreaterThanOrEqualTo =>
        cm.withFallbackColor(colorRamp.colors(0))
      case LessThan | LessThanOrEqualTo =>
        cm.withFallbackColor(colorRamp.colors(colorRamp.colors.length - 1))
      case Exact =>
        cm
    }
  }

  /** Parse an integer color map from a string of value:color;value:color;... */
  def fromString(str: String): Option[ColorMap] = {
    val split = str.split(';').map(_.trim.split(':'))
    Try {
      val limits = split.map { pair => pair(0).toInt }
      val colors = split.map { pair => BigInt(pair(1), 16).toInt }
      require(limits.size == colors.size)
      apply(limits, colors)
    }.toOption
  }

  /** Parse an integer color map from a string of value:color;value:color;... */
  def fromStringDouble(str: String): Option[ColorMap] = {
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

  def render(tile: Tile): Tile = {
    val result = ArrayTile.empty(IntCellType, tile.cols, tile.rows)
    if(tile.cellType.isFloatingPoint) {
      tile.foreachDouble { (col, row, z) =>
        result.setDouble(col, row, mapDouble(z))
      }
    } else {
      tile.foreach { (col, row, z) =>
        result.set(col, row, map(z))
      }
    }
    result
  }

  def map(v: Int): Int
  def mapDouble(v: Double): Int

  def mapColors(f: Int => Int): ColorMap
  /** Maps each color value to its index in the [[colors]] sequence.
    * This is useful for table lookup encodings (ex: [[geotrellis.raster.render.png.IndexedPngEncoding]]) */
  def mapColorsToIndex(): ColorMap

  def withNoDataColor(color: Int): ColorMap
  def withFallbackColor(color: Int): ColorMap
  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap

  /** Retrieve a "breaks string" from [[ColorMap]] data.
    * The opposite of the [[ColorMap.fromString]] methods.
    */
  def breaksString: String
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

  lazy val colors: Vector[Int] = orderedBreaks.map(breaksToColors(_))

  private val breakMap: BreakMap[Int, Int] =
    new BreakMap(breaksToColors, options.strategy, { i => isNoData(i) })

  def map(z: Int): Int = breakMap(z)

  def mapDouble(z: Double): Int = map(d2i(z))

  def mapColors(f: Int => Int): ColorMap =
    new IntColorMap(breaksToColors.map { case (key, color) => (key, f(color)) }, options)

  def mapColorsToIndex(): ColorMap =
    new IntColorMap(orderedBreaks.zipWithIndex.toMap, options)

  def withNoDataColor(color: Int): ColorMap =
    new IntColorMap(breaksToColors, options.copy(noDataColor = color))

  def withFallbackColor(color: Int): ColorMap =
    new IntColorMap(breaksToColors, options.copy(fallbackColor =  color))

  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap =
    new IntColorMap(breaksToColors, options.copy(classBoundaryType = classBoundaryType))

  def cache(h: Histogram[Int]): ColorMap = {
    val ch = h.mutable
    val cachedColors = h.values()
    cfor(0)( _ < cachedColors.length, _ + 1) { i =>
      val z = cachedColors(i)
      val itemColor = map(z)
      cachedColors(i) = itemColor
      ch.setItem(z, itemColor)
    }
    // multiple values could have mapped to same color, use only distinct color values
    new IntCachedColorMap(cachedColors.distinct.toVector, ch, options)
  }

  def breaksString: String = {
    breaksToColors
      .toStream
      .map({ case (k,v) => s"${k}:${Integer.toHexString(v)}"})
      .mkString(";")
  }
}

/** Caches a color ramp based on a histogram of values. This is an optimization, since
  * often times we create a histogram while classifying, and can reuse that computed
  * information in the color mapping.
  *
  * In order for this class to work correctly the histogram acts as a map from values to their colors
  * and must contain a value for each pixel value that we expect to encounter.
  *
  * The performance benefit is `eC` lookup cost instead of `Log` provided by [[IntColorMap]]
  *
  * @param colors All color values that can be encountered
  * @param h      Histogram where counts of values have been replaced cached RGBA color
  */
class IntCachedColorMap(val colors: Vector[Int], h: Histogram[Int], val options: Options)
    extends ColorMap {
  val noDataColor = options.noDataColor

  def map(z: Int): Int = { if(isNoData(z)) noDataColor else h.itemCount(z).toInt }

  def mapDouble(z: Double): Int = map(d2i(z))

  def mapColors(f: Int => Int): ColorMap = {
    val ch = h.mutable
    h.foreachValue(z => ch.setItem(z, f(h.itemCount(z).toInt)))
    new IntCachedColorMap(colors, ch, options)
  }

  def mapColorsToIndex(): ColorMap = {
    val colorIndexMap = colors.zipWithIndex.toMap
    val ch = h.mutable

    h.foreachValue(z => ch.setItem(z, colorIndexMap(h.itemCount(z).toInt)))
    new IntCachedColorMap((0 to colors.length).toVector, ch, options)
  }

  def withNoDataColor(color: Int): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(noDataColor = color))

  def withFallbackColor(color: Int): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(fallbackColor = color))

  def withBoundaryType(classBoundaryType: ClassBoundaryType): ColorMap =
    new IntCachedColorMap(colors, h, options.copy(classBoundaryType = classBoundaryType))

  def breaksString: String = {
    h.quantileBreaks(colors.length)
      .toVector
      .zip(colors)
      .map({ case (k,v) => s"${k}:${Integer.toHexString(v)}"})
      .mkString(";")
  }
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

  lazy val colors = orderedBreaks.map(breaksToColors(_))

  private val breakMap: BreakMap[Double, Int] =
    new BreakMap(breaksToColors, options.strategy, { d => isNoData(d) })

  def map(z: Int): Int = mapDouble(i2d(z))

  def mapDouble(z: Double): Int = breakMap(z)

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

  def breaksString: String = {
    breaksToColors
      .toStream
      .map({ case (k,v) => s"${k}:${Integer.toHexString(v)}"})
      .mkString(";")
  }
}

/** A color map where the breaks are monotonically increasing integer values starting at zero.
 * Primarily used for capturing and persisting indexed color maps in GeoTIFFs. */
class IndexedColorMap(indexedColors: Seq[Int]) extends IntColorMap(
  indexedColors.zipWithIndex.map(p ⇒ p._2 -> p._1).toMap
) {
  override def toString = getClass.getSimpleName + "(" +
    colors.map(c ⇒ f"0x$c%02x").mkString(", ") + ")"
}

object IndexedColorMap {
  // As with GDAL, we need to pack each 16 bit color channel into 8 bits.
  // GDAL just shifts and masks, effectively converting a 0-65535 range
  // to a 0-255 one. These conversion approaches are similar to those used
  // in the GDAL TIFF driver.
  private def downsample(c: Int) = (c >> 8 & 0xFF).toShort
  private def upsample(c: Int) = (c * 257).toShort

  /** Creates an IndexColorMap from sequence of RGB short values. */
  def fromTiffPalette(tiffPalette: Seq[(Short, Short, Short)]) = new IndexedColorMap(
    tiffPalette.map { case (red, green, blue) ⇒ RGB(downsample(red), downsample(green), downsample(blue))}
  )
  /** Converts a ColorMap to sequence of short triplets in encoding expected by GeoTiff 'Palette' color space.*/
  def toTiffPalette(cm: ColorMap): Seq[(Short, Short, Short)] =
    fromColorMap(cm).colors.map(c ⇒ (upsample(c.red), upsample(c.green), upsample(c.blue)))

  /** Flattens the given colormap into an indexed variant, throwing away any defined boundaries. */
  def fromColorMap(cm: ColorMap) = new IndexedColorMap(cm.colors)
}
