package trellis.operation

import trellis._
import trellis.geometry.Polygon
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._

/**
 * Rasterize a polygon and then draw it on the provided raster.
 */
case class BurnPolygon(r:Op[IntRaster], p:Op[Polygon]) extends Op2(r,p) ({
  (raster,polygon) => {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, Array(polygon))
    Result(copy)
  }
})
