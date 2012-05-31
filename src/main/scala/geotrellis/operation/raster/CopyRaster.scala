package geotrellis.operation

import geotrellis.process._
import geotrellis.Raster

/**
 * Given an operation producing a raster, returns a copy of this raster.
 *
 * Useful because some operations currently mutate one or more of their
 * arguments.
 */
case class CopyRaster(r:Op[Raster]) extends Op1(r)({
  (r) => Result(r.copy)
})
