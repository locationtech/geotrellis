package geotrellis.io

import geotrellis.data.png._
import geotrellis.data._
import geotrellis.process._
import geotrellis._
import geotrellis._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
 */
case class RenderPNG(r:Op[Raster], colorBreaks:Op[ColorBreaks],
                     noDataColor:Op[Int], transparent:Op[Boolean])
extends Op4(r,colorBreaks,noDataColor, transparent)({
  (r, breaks, noDataColor, transparent) => {
    val mapper = ColorMapper(breaks, 0)
    val ndc = if (transparent) 0 else noDataColor
    val r2 = r.convert(TypeInt).map(n => if (n == NODATA) ndc else (mapper(n) << 8) | 255)
    Result(RgbaEncoder().writeByteArray(r2))
  }
})

object RenderPNG3 {
  def apply(r:Op[Raster], mapper:Op[ColorMapper]) = RenderPng32(r, mapper)
}

object RenderPng32 {
  def apply(r:Op[Raster], mapper:Op[ColorMapper]) =
    RenderPngRgba(ReclassifyColorsRgba(r, mapper))
}

case class RenderPngRgba(r:Op[Raster]) extends Op1(r) ({
  r => Result(RgbaEncoder().writeByteArray(r))
})

case class ReclassifyColorsRgba(r:Op[Raster], mapper:Op[ColorMapper]) extends Op2(r, mapper) ({
  (r, mapper) => Result(r.map(n => if (n == NODATA) 0 else (mapper(n) << 8) + 255))
})
