package geotrellis.render.op

import geotrellis._
import geotrellis.render._
import geotrellis.render.png._

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
case class RenderPng(r:Op[Raster], colorBreaks:Op[ColorBreaks], noDataColor:Op[Int])
extends Op3(r, colorBreaks, noDataColor)({
  (r, colorBreaks, noDataColor) => {
    val breaks = colorBreaks.limits
    val colors = colorBreaks.colors

    val renderer = Renderer(breaks, colors, noDataColor)
    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writeByteArray(r2)

    Result(bytes)
  }
})
