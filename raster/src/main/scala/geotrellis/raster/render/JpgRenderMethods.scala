package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.jpg._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.op.stats._

trait JpgRenderMethods extends TileMethods {
  /** Generate a JPG from a raster of RGBA integer values.
    *
    * Use this operation when you have created a raster whose values are already
    * RGBA color values that you wish to render into a JPG. If you have a raster
    * with data that you wish to render, you should use RenderJpg instead.
    *
    * An RGBA value is a 32 bit integer with 8 bits used for each component:
    * the first 8 bits are the red value (between 0 and 255), then green, blue,
    * and alpha (with 0 being transparent and 255 being opaque).
    */
  def renderJpg(): Jpg =
    new JpgEncoder().writeByteArray(tile)

  def renderJpg(settings: jpg.Settings) =
    new JpgEncoder(settings).writeByteArray(tile)

  def renderJpg(colorRamp: ColorRamp): Jpg =
    renderJpg(colorRamp.toArray)

  /**
    * Generate a JPG image from a raster.
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
  def renderJpg(colorBreaks: ColorBreaks): Jpg =
    renderJpg(colorBreaks, None)

  /**
    * Generate a JPG image from a raster.
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
  def renderJpg(colorBreaks: ColorBreaks, histogram: Histogram): Jpg =
    renderJpg(colorBreaks, Some(histogram))

  private
  def renderJpg(colorBreaks: ColorBreaks, histogram: Option[Histogram]): Jpg = {
    val renderer =
      histogram match {
        case Some(h) => Renderer(colorBreaks, h)
        case None => Renderer(colorBreaks)
      }

    val r2 = renderer.render(tile)
    new JpgEncoder().writeByteArray(r2)
  }

  def renderJpg(ramp: ColorRamp, breaks: Array[Int]): Jpg =
    renderJpg(ColorBreaks(breaks, ramp.toArray))

  def renderJpg(colors: Array[Int]): Jpg = {
    val h = tile.histogram
    renderJpg(ColorBreaks(h, colors), h)
  }

  def renderJpg(colors: Array[Int], numColors: Int): Jpg =
    renderJpg(Color.chooseColors(colors, numColors))

  def renderJpg(breaks: Array[Int], colors: Array[Int]): Jpg =
    renderJpg(ColorBreaks(breaks, colors))

  def renderJpg(breaks: Array[Int], colors: Array[Int], noDataColor: Int): Jpg =
    renderJpg(ColorBreaks(breaks, colors, Some(noDataColor)))

  def renderJpg(breaks: Array[Double], colors: Array[Int]): Jpg =
    renderJpg(ColorBreaks(breaks, colors))

  def renderJpg(breaks: Array[Double], colors: Array[Int], noDataColor: Int): Jpg =
    renderJpg(ColorBreaks(breaks, colors, Some(noDataColor)))
}
