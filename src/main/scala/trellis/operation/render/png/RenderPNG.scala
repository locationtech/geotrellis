package trellis.operation


import trellis.data._
import trellis.process._
import trellis._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
 */
case class RenderPNG(r:Op[IntRaster], colorBreaks:Op[ColorBreaks], noDataColor:Op[Int], transparent:Op[Boolean])
extends Op4(r,colorBreaks,noDataColor, transparent)({
  (raster, breaks, noDataColor, transparent) => {
    val f = ColorMapper(breaks, noDataColor)
    val w = new PNGRenderer(raster, "/dev/null", f, noDataColor, transparent)
    Result(w.render)
  }
})

//TODO: documentation, rename
case class RenderPNG3(r:Op[IntRaster], mapper:Op[ColorMapper]) extends Op2(r,mapper) ({
  (raster,mapper) => {
    val w = new PNGRenderer(raster, "/dev/null", mapper, mapper.nodataColor, true)
    Result(w.render)
  }
})
