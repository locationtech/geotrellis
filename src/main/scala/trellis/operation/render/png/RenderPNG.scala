package trellis.operation

import trellis.constant._
import trellis.data._
import trellis.process._
import trellis.raster._

/**
 * Generate a PNG from a given raster and a set of color breaks. The background
 * can be set to a color or be made transparent.
 */
case class RenderPNG(r:Op[IntRaster], cbs:Op[ColorBreaks], ndc:Op[Int], t:Op[Boolean])
extends Op[Array[Byte]] {
  def _run(context:Context) = runAsync(List(r, cbs, ndc, t))

  val nextSteps:Steps = {
    case ((raster:IntRaster) :: (breaks:ColorBreaks) :: (noDataColor:Int) :: (transparent:Boolean) :: Nil) => {
      step2(raster, breaks, noDataColor, transparent)
    }
  }

  def step2(raster:IntRaster, breaks:ColorBreaks, noDataColor:Int, transparent:Boolean) = {
    val f = ColorMapper(breaks, noDataColor)
    val w = new PNGRenderer(raster, "/dev/null", f, noDataColor, transparent)
    Result(w.render)
  }
}

case class RenderPNG3(r:Op[IntRaster], m:Op[ColorMapper]) extends Op[Array[Byte]] {
  def _run(context:Context) = runAsync(List(r, m))

  val nextSteps:Steps = {
    case (raster:IntRaster) :: (mapper:ColorMapper) :: Nil => step2(raster, mapper)
  }

  def step2(raster:IntRaster, f:ColorMapper) = {
    val w = new PNGRenderer(raster, "/dev/null", f, f.nodataColor, true)
    Result(w.render)
  }
}
