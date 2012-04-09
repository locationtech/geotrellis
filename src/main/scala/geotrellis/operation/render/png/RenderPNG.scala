package geotrellis.operation

import geotrellis.data.png._
import geotrellis.data._
import geotrellis.process._
import geotrellis._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
 */
case class RenderPNG(r:Op[IntRaster], colorBreaks:Op[ColorBreaks], noDataColor:Op[Int], transparent:Op[Boolean])
extends Op4(r,colorBreaks,noDataColor, transparent)({
  (r, breaks, noDataColor, transparent) => {
    val f = ColorMapper(breaks, noDataColor)
    Result(RgbaEncoder().writeByteArray(r.map(f)))
  }
})

//TODO: documentation, rename
case class RenderPNG3(r:Op[IntRaster], mapper:Op[ColorMapper]) extends Op2(r,mapper) ({
  (r, f) => Result(RgbaEncoder().writeByteArray(r.map(f)))
})
