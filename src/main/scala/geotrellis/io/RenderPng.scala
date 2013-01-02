package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics.op._
import geotrellis.statistics.Histogram


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
 *   val histogramOp = stat.GetHistogram(r)
 *   val colors = Array(0xFF0000FF, 0x00FF00FF) // red and green in RGBA values
 *   // generate a 6 color gradient between red and green
 *   val colorsOp = stat.GetColorsFromPalette(colors, 6) 
 *   val breaksOp = stat.GetColorBreaks(histogramOp, numColorsOp)
 *   val pngOp = io.RenderPng(rOp, braeksOp, histogramOp, 0)
 * }}}
 */
case class RenderPng(r:Op[Raster], colorBreaks:Op[ColorBreaks], h:Op[Histogram],
                     noDataColor:Op[Int])
extends Op4(r, colorBreaks, h, noDataColor)({
  (r, colorBreaks, h, noDataColor) => {
    val breaks = colorBreaks.limits
    val colors = colorBreaks.colors
    val renderer = Renderer(breaks, colors, h, noDataColor)
    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writeByteArray(r2)
    Result(bytes)
  }
})

