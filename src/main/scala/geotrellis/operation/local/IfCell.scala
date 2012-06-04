package geotrellis.operation

import geotrellis.process._
import geotrellis._

/**
 * Maps all cells matching `cond` to `trueValue`.
 */
case class IfCell(r:Op[Raster], cond:Int => Boolean, trueValue:Int) extends Op1(r)({
  (r) => Result(r.map(z => if (cond(z)) trueValue else z)) 
}) 

object IfCell {
  def apply(r:Op[Raster], cond:Int => Boolean, trueValue:Int, falseValue: Int) = {
    IfElseCell(r, cond, trueValue, falseValue)
  }

  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int) = {
    new BinaryIfCell(r1, r2, cond, trueValue)
  }

  def apply(r1:Op[Raster], r2:Op[Raster], cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = {
    new BinaryIfElseCell(r1, r2, cond, trueValue, falseValue)
  }
}
