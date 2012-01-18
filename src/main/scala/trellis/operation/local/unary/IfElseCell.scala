package trellis.operation

import trellis._
import trellis.process._

/**
 * Set all values of output raster to one value or another based on whether a
 * condition is true or false.
 */
case class IfElseCell(r:Op[IntRaster], cond:Int => Boolean, trueValue:Int,
                      falseValue:Int) extends UnaryLocal {
  def getCallback(context:Context) = (z:Int) => if (cond(z)) {
    trueValue
  } else {
    falseValue
  }
}
