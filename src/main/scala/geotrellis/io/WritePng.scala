package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.statistics._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
case class WritePng(r:Op[Raster], path:Op[String],
                    colorBreaks:Op[ColorBreaks], h:Op[Histogram],
                    noDataColor:Op[Int])
extends Op5(r, path, colorBreaks, h, noDataColor) ({
    (r, path, colorBreaks, h, noDataColor) => {
    val breaks = colorBreaks.limits
    val colors = colorBreaks.colors
    val renderer = Renderer(breaks, colors, h, noDataColor)
    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writePath(path, r2)
    Result(())
  }
})

/**
 * Write out a PNG file of a raster that contains RGBA values)
 */
case class WritePngRgba(r:Op[Raster], path:Op[String]) extends Op2(r, path)({
  (r, path) =>
    val bytes = new Encoder(Settings(Rgba, PaethFilter)).writePath(path, r)
    Result(bytes)
})
