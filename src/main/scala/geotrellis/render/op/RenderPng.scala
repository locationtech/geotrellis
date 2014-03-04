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

package geotrellis.render.op

import geotrellis._
import geotrellis.render._
import geotrellis.render.png._
import geotrellis.statistics.Histogram

object RenderPng {
  def apply(r:Op[Raster], colorBreaks:Op[ColorBreaks], noDataColor:Op[Int]):RenderPng =
    RenderPng(r,colorBreaks,noDataColor,None)

  def apply(r:Op[Raster], colorBreaks:Op[ColorBreaks], noDataColor:Op[Int],h:Histogram):RenderPng =
    RenderPng(r,colorBreaks,noDataColor,Some(h))
}

/**
 * Generate a PNG image from a raster.
 *
 * Use this operation when you have a raster of data that you want to visualize
 * with an image.
 *
 * To render a data raster into an image, the operation needs to know which
 * values should be painted with which colors.  To that end, you'll need to
 * generate a ColorBreaks object which represents the value ranges and the
 * assigned color.  One way to create these color breaks is to use the
 * [[geotrellis.statistics.op.stat.GetClassBreaks]] operation to generate
 * quantile class breaks. 
 *
 * Example usage:
 * {{{
 *   import geotrellis.statistics.op._
 *
 *   val rOp = io.LoadRaster("foo") // get a data raster
 *   val colors = Array(0xFF0000FF, 0x00FF00FF) // red and green in RGBA values
 *   // generate a 6 color gradient between red and green
 *   val colorsOp = stat.GetColorsFromPalette(colors, 6) 
 *   val breaksOp = stat.GetColorBreaks(histogramOp, numColorsOp)
 *   val pngOp = io.RenderPng(rOp, braeksOp, 0)
 * }}}
 */
case class RenderPng(r:Op[Raster], 
                     colorBreaks:Op[ColorBreaks], 
                     noDataColor:Op[Int],
                     histogram:Option[Histogram])
extends Op3(r, colorBreaks, noDataColor)({
  (r, colorBreaks, noDataColor) => {
    val breaks = colorBreaks.limits
    val colors = colorBreaks.colors

    val renderer = 
      histogram match {
        case Some(h) => Renderer(breaks, colors, noDataColor,h)
        case None => Renderer(breaks, colors, noDataColor)
      }

    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writeByteArray(r2)

    Result(bytes)
  }
})
