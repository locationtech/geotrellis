package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis._

/**
 * Forces evaluation of the resulting raster of the passed in operation.
 * This is useful when you want to force lazy operations to happen
 * at specific points in the operation chain.
 */
// case class Force(r:Op[Raster]) extends Op1(r)({
//   r => Result(r.force)
// })
