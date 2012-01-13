package trellis.operation

import trellis.process._
import trellis.raster._

/**
 * Maps all cells matching `cond` to `trueValue`.
 */
case class IfCell(r:Op[IntRaster], cond:Int => Boolean, trueValue:Int) extends UnaryLocal {
  def getCallback(context:Context) = (z:Int) => if (cond(z)) trueValue else z
} 

object IfCell {
  def apply(r:Op[IntRaster], cond:Int => Boolean, trueValue:Int, falseValue: Int) = {
    IfElseCell(r, cond, trueValue, falseValue)
  }

  def apply(r1:Op[IntRaster], r2:Op[IntRaster], cond:(Int, Int) => Boolean, trueValue:Int) = {
    new BinaryIfCell(r1, r2, cond, trueValue)
  }

  def apply(r1:Op[IntRaster], r2:Op[IntRaster], cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = {
    new BinaryIfElseCell(r1, r2, cond, trueValue, falseValue)
  }
}
