/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.render.png

import geotrellis._
import geotrellis.render._
import geotrellis.statistics.Histogram

import scala.collection.mutable

case class Renderer(colorMap:ColorMap, rasterType:RasterType, colorType:ColorType) {
  def render(r:Raster) = 
    colorMap.render(r).convert(rasterType)
  def settings = Settings(colorType, PaethFilter)
}

object Renderer {
  def apply(breaks:ColorBreaks, nodata:Int):Renderer =
    apply(breaks.limits, breaks.colors, nodata)

  def apply(limits:Array[Int], colors:Array[Int], nodata:Int):Renderer =
    apply(limits,colors,nodata,None)

  def apply(limits:Array[Int], colors:Array[Int], nodata:Int,h:Histogram):Renderer =
    apply(limits,colors,nodata,Some(h))

  /** Include a precomputed histogram to cache the color map and speed up the rendering. */
  def apply(limits:Array[Int], colors:Array[Int], nodata:Int,h:Option[Histogram]):Renderer = {
    val n = limits.length
    if(colors.length < 255) {
      val indices = (0 until colors.length).toArray
      val rgbs = new Array[Int](256)
      val as = new Array[Int](256)

      var i = 0
      while (i < n) {
        val c = colors(i)
        rgbs(i) = c >> 8
        as(i) = c & 0xff
        i += 1
      }
      rgbs(255) = 0
      as(255) = 0
      val colorType = Indexed(rgbs, as)
      val colorMap = ColorMap(limits,indices,ColorMapOptions(LessThan,255))
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
      while (i < colors.length) {
        val c = colors(i)
        opaque &&= Color.isOpaque(c)
        grey &&= Color.isGrey(c)
        i += 1
      }

      if (grey && opaque) {
        val colorMap = ColorMap(limits,colors.map(z => (z >> 8) & 0xff),ColorMapOptions(LessThan,nodata))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeByte, Grey(nodata))
          case None =>
            Renderer(colorMap, TypeByte, Grey(nodata))
        }
      } else if (opaque) {
        val colorMap = ColorMap(limits,colors.map(z => z >> 8),ColorMapOptions(LessThan,nodata))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeInt, Rgb(nodata))
          case None =>
            Renderer(colorMap, TypeInt, Rgb(nodata))
        }
      } else if (grey) {
        val colorMap = ColorMap(limits,colors.map(z => z & 0xffff),ColorMapOptions(LessThan,nodata))
        h match {
          case Some(hist) =>
            Renderer(colorMap.cache(hist), TypeShort, Greya)
          case None =>
            Renderer(colorMap, TypeShort, Greya)
        }
      } else {
        val colorMap = ColorMap(limits,colors,ColorMapOptions(LessThan,nodata))
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
