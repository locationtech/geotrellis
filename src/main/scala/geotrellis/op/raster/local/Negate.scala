package geotrellis.op.raster.local

import geotrellis._
import geotrellis.op._
import geotrellis.process._

/**
 * Negate (multiply by -1) each value in a raster.
 */
case class Negate(r:Op[Raster]) extends Op1(r)({
  (r) => Result(r.map( z => -z ))
})
