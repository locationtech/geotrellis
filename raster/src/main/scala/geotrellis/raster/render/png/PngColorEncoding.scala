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

package geotrellis.raster.render.png

import geotrellis.raster.render._

/** Captures the conversion strategies from RGBA color space to PNG pixels. */
sealed abstract class PngColorEncoding(val n: Byte, val depth: Int) {
 def convertColorMap(colorMap: ColorMap): ColorMap
}

// greyscale and color opaque rasters
case class GreyPngEncoding(transparent: Option[Int]) extends PngColorEncoding(0, 1) {
 def convertColorMap(colorMap: ColorMap): ColorMap =
   colorMap.mapColors { c => c.blue }
}
trait GreyPngEncodingConvertable { implicit def toGreyPngEncoding(self: GreyPngEncodingConvertable): GreyPngEncoding = GreyPngEncoding() }
object GreyPngEncoding extends GreyPngEncodingConvertable {
  def apply(transparent: Int): GreyPngEncoding = GreyPngEncoding(Some(transparent))
  def apply(): GreyPngEncoding = GreyPngEncoding(None)
}

case class RgbPngEncoding(transparent: Option[Int]) extends PngColorEncoding(2, 3) {
 def convertColorMap(colorMap: ColorMap): ColorMap =
   colorMap.mapColors { c => c >> 8 }
}
trait RgbPngEncodingConvertable { implicit def toRgbPngEncoding(self: RgbPngEncodingConvertable): RgbPngEncoding = RgbPngEncoding() }
object RgbPngEncoding extends RgbPngEncodingConvertable {
  def apply(transparent: Int): RgbPngEncoding = RgbPngEncoding(Some(transparent))
  def apply(): RgbPngEncoding = RgbPngEncoding(None)
}

// indexed color, using separate rgb and alpha channels
case class IndexedPngEncoding(rgbs: Array[Int], as: Array[Int]) extends PngColorEncoding(3, 1) {
 def convertColorMap(colorMap: ColorMap): ColorMap =
   colorMap.mapColorsToIndex().withNoDataColor(255).withFallbackColor(254)
}

// greyscale and color rasters with an alpha byte
case object GreyaPngEncoding extends PngColorEncoding(4, 2) {
 def convertColorMap(colorMap: ColorMap): ColorMap =
   colorMap.mapColors { c => c.int & 0xffff }
}

case object RgbaPngEncoding extends PngColorEncoding(6, 4) {
 def convertColorMap(colorMap: ColorMap): ColorMap =
   colorMap
}

object PngColorEncoding {
  def apply(colors: Vector[Int], noDataColor: Int, fallbackColor: Int): PngColorEncoding = {
    val len = colors.length

    // indexed PNGs can have up to 254 mapped colors (to leave room for nodata and fallback)
    if(len <= 254) {
      // PNG header lookup array
      val rgbs = new Array[Int](256)
      val as = new Array[Int](256)

      // Produce the array to be stored in the PNG header for color lookup
      var i = 0
      while (i < len) {
        val c = colors(i)
        rgbs(i) = c.toARGB
        as(i) = c.alpha
        i += 1
      }

      // Fallback index
      rgbs(254) = fallbackColor.toARGB
      as(254) = fallbackColor.alpha

      // NoData index
      rgbs(255) = noDataColor.toARGB
      as(255) = noDataColor.alpha
      IndexedPngEncoding(rgbs, as)
    } else {
      var opaque = true
      var grey = true
      var i = 0
      while (i < len) {
        val c = colors(i)
        opaque &&= c.isOpaque
        grey &&= c.isGrey
        i += 1
      }
      opaque &&= fallbackColor.isOpaque
      grey &&= fallbackColor.isGrey
      opaque &&= noDataColor.isOpaque
      grey &&= noDataColor.isGrey

      if (grey && opaque) {
        GreyPngEncoding(noDataColor.int)
      } else if (opaque) {
        RgbPngEncoding(noDataColor.int)
      } else if (grey) {
        GreyaPngEncoding
      } else {
        RgbaPngEncoding
      }
    }
  }
}
