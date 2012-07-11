package geotrellis.op.local

import geotrellis._
import geotrellis.op._
import geotrellis.process._

/**
 * Set all values of output raster to one value or another based on whether a
 * condition is true or false.
 */
case class IfElseCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int,
                      falseValue:Int) extends Op1(r)({
  (r) => Result(r.map(z => if (cond(z)) trueValue else falseValue))
})
