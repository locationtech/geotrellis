package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.render.shared._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.op.stats._

trait PngRenderMethods extends TileMethods {
  /** Generate a PNG from a raster of RGBA integer values.
    *
    * Use this operation when you have created a raster whose values are already
    * RGBA color values that you wish to render into a PNG. If you have a raster
    * with data that you wish to render, you should use RenderPng instead.
    *
    * An RGBA value is a 32 bit integer with 8 bits used for each component:
    * the first 8 bits are the red value (between 0 and 255), then green, blue,
    * and alpha (with 0 being transparent and 255 being opaque).
    */
  def renderPng(): Png =
    new PngEncoder(Settings(Rgba, PaethFilter)).writeByteArray(tile)

  def renderPng(colorRamp: ColorRamp): Png =
    renderPng(colorRamp.toArray)

  def renderPng(colorBreaks: ColorBreaks): Png =
    renderPng(colorBreaks, 0)

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
    * [[geotrellis.raster.stats.op.stat.GetClassBreaks]] operation to generate
    * quantile class breaks.
    */
  def renderPng(colorBreaks: ColorBreaks, noDataColor: Int): Png =
    renderPng(colorBreaks, noDataColor, None)

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
    * [[geotrellis.raster.stats.op.stat.GetClassBreaks]] operation to generate
    * quantile class breaks.
    */
  def renderPng(colorBreaks: ColorBreaks, noDataColor: Int, histogram: Histogram): Png =
    renderPng(colorBreaks, noDataColor, Some(histogram))

  private
  def renderPng(colorBreaks: ColorBreaks, noDataColor: Int, histogram: Option[Histogram]): Png = {
    val renderer =
      histogram match {
        case Some(h) => Renderer(colorBreaks, noDataColor, h)
        case None => Renderer(colorBreaks, noDataColor)
      }

    val r2 = renderer.render(tile)
    new PngEncoder(renderer.settings).writeByteArray(r2)
  }

  def renderPng(ramp: ColorRamp, breaks: Array[Int]): Png =
    renderPng(ColorBreaks(breaks, ramp.toArray))

  def renderPng(colors: Array[Int]): Png = {
    val h = tile.histogram
    renderPng(ColorBreaks(h, colors), 0, h)
  }

  def renderPng(colors: Array[Int], numColors: Int): Png =
    renderPng(Color.chooseColors(colors, numColors))

  def renderPng(breaks: Array[Int], colors: Array[Int]): Png =
    renderPng(ColorBreaks(breaks, colors), 0)

  def renderPng(breaks: Array[Int], colors: Array[Int], noDataColor: Int): Png =
    renderPng(ColorBreaks(breaks, colors), noDataColor)

  def renderPng(breaks: Array[Double], colors: Array[Int]): Png =
    renderPng(ColorBreaks(breaks, colors), 0)

  def renderPng(breaks: Array[Double], colors: Array[Int], noDataColor: Int): Png =
    renderPng(ColorBreaks(breaks, colors), noDataColor)
}
