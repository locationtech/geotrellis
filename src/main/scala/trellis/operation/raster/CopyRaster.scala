package trellis.operation

import trellis.process._
import trellis.raster.IntRaster

/**
 * Given an operation producing a raster, returns a copy of this raster.
 *
 * Useful because some operations currently mutate one or more of their
 * arguments.
 */
case class CopyRaster(r:Op[IntRaster]) extends Op[IntRaster] {
  def _run(context:Context) = runAsync(List(r))
  val nextSteps:Steps = { case (r:IntRaster) :: Nil => Result(r.copy) }
}
