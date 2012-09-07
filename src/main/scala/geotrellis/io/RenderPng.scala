package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
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

case class RenderPngRgba(r:Op[Raster]) extends Op1(r)({
  r =>
    val bytes = new Encoder(Settings(Rgba, PaethFilter)).writeByteArray(r)
    Result(bytes)
})
