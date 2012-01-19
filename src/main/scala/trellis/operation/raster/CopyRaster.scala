package trellis.operation

import trellis.process._
import trellis.IntRaster

/**
 * Given an operation producing a raster, returns a copy of this raster.
 *
 * Useful because some operations currently mutate one or more of their
 * arguments.
 */
case class CopyRaster(r:Op[IntRaster]) extends Op1(r)({
  (r) => Result(r.copy)
})
