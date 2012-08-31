package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
 */
case class RenderPng(r:Op[Raster], colorBreaks:Op[ColorBreaks],
                     noDataColor:Op[Int], transparent:Op[Boolean])
extends Op4(r,colorBreaks,noDataColor, transparent)({
  (r, colorBreaks, noDataColor, transparent) => {
    val breaks = colorBreaks.breaks.map(_._1)
    val colors = colorBreaks.breaks.map(_._2)
    val renderer = Renderer(breaks, colors, noDataColor)
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
