package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics._

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

/**
 * Generate a PNG from a raster of RGBA integer values.
 *
 * Use this operation when you have created a raster whose values are already
 * RGBA color values that you wish to render into a PNG.  If you have a raster
 * with data that you wish to render, you should use RenderPng instead.
 *
 * An RGBA value is a 32 bit integer with 8 bits used for each component:
 * the first 8 bits are the red value (between 0 and 255), then green, blue,
 * and alpha (with 0 being transparent and 255 being opaque).
 */
case class RenderPngRgba(r:Op[Raster]) extends Op1(r)({
  r =>
    val bytes = new Encoder(Settings(Rgba, PaethFilter)).writeByteArray(r)
    Result(bytes)
})
