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

import  geotrellis.raster.render.jpg._
import  geotrellis.raster.render.png._
import geotrellis._
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.histogram.Histogram

import scala.collection.mutable

case class Renderer(colorMap: ColorMap, cellType: CellType, colorType: ColorType) {
  def render(r: Tile) =
    colorMap.render(r).convert(cellType)
}

object Renderer {
  def apply(colorBreaks: ColorBreaks): Renderer =
    apply(colorBreaks, None)

  def apply(limits: Array[Int], colors: Array[Int]): Renderer =
    apply(ColorBreaks(limits, colors), None)

  def apply(limits: Array[Double], colors: Array[Int]): Renderer =
    apply(ColorBreaks(limits, colors), None)

  def apply(limits: Array[Int], colors: Array[Int], nodata: Int): Renderer =
    apply(ColorBreaks(limits, colors, Some(nodata)), None)

  def apply(limits: Array[Double], colors: Array[Int], nodata: Int): Renderer =
    apply(ColorBreaks(limits, colors, Some(nodata)), None)

  def apply(limits: Array[Int], colors: Array[Int], nodata: Int, h: Histogram): Renderer =
    apply(ColorBreaks(limits, colors, Some(nodata)), Some(h))

  def apply(limits: Array[Double], colors: Array[Int], nodata: Int, h: Histogram): Renderer =
    apply(ColorBreaks(limits, colors, Some(nodata)), Some(h))

  def apply(colorBreaks: ColorBreaks, h: Histogram): Renderer =
    apply(colorBreaks, Some(h))

  /** Include a precomputed histogram to cache the color map and speed up the rendering. */
  def apply(colorBreaks: ColorBreaks, h: Option[Histogram]): Renderer = {
    val ndColor = colorBreaks.ndColor.getOrElse(0)
    val len = colorBreaks.length
    if(len <= 256) {
      val indices = (0 until len).toArray
      val rgbs = new Array[Int](256)
      val as = new Array[Int](256)

      var i = 0
      while (i < len) {
        val c = colorBreaks.colors(i)
        rgbs(i) = c >> 8
        as(i) = c & 0xff
        i += 1
      }
      rgbs(255) = 0
      as(255) = 0
      val colorType = Indexed(rgbs, as)
      val colorMap = colorBreaks.replaceColors(indices).toColorMap(ColorMapOptions(LessThan, 255))
      h match {
        case Some(hist) =>
          Renderer(colorMap.cache(hist), TypeByte, colorType)
        case None =>
          Renderer(colorMap, TypeByte, colorType)
      }
    } else {

      var opaque = true
      var grey = true
      var i = 0
      while (i < len) {
        val c = colorBreaks.colors(i)
        opaque &&= Color.isOpaque(c)
        grey &&= Color.isGrey(c)
        i += 1
      }

      if (grey && opaque) {
        val colorMap = colorBreaks.mapColors { z => (z >> 8) & 0xff }.toColorMap(ColorMapOptions(LessThan, ndColor))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeByte, Grey(ndColor))
          case None =>
            Renderer(colorMap, TypeByte, Grey(ndColor))
        }
      } else if (opaque) {
        val colorMap = colorBreaks.mapColors { z => z >> 8 }.toColorMap(ColorMapOptions(LessThan, ndColor))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeInt, Rgb(ndColor))
          case None =>
            Renderer(colorMap, TypeInt, Rgb(ndColor))
        }
      } else if (grey) {
        val colorMap = colorBreaks.mapColors { z => z & 0xffff }.toColorMap(ColorMapOptions(LessThan, ndColor))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeShort, Greya)
          case None =>
            Renderer(colorMap, TypeShort, Greya)
        }
      } else {
        val colorMap = colorBreaks.toColorMap(ColorMapOptions(LessThan, ndColor))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeInt, Rgba)
          case None =>
            Renderer(colorMap, TypeInt, Rgba)
        }
      }
    }
  }
}
