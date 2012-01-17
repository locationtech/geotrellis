package trellis.operation

import trellis._
import trellis.geometry.Polygon
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._

/**
 * Rasterize a polygon and then draw it on the provided raster.
 */
case class BurnPolygon(r:Op[IntRaster], p:Op[Polygon]) extends Op[IntRaster] {
  def _run(context:Context) = runAsync(List(r, p))

  val nextSteps:Steps = {
    case (raster:IntRaster) :: (polygon:Polygon) :: Nil => step2(raster, polygon)
  }

  def step2(raster:IntRaster, polygon:Polygon) = {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, Array(polygon))
    Result(copy)
  }
}
