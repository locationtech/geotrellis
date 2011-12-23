package trellis.operation

/**
 * Maps all cells matching `cond` to `trueValue`.
 */
case class IfCell(r:IntRasterOperation, cond:Int => Boolean, 
                  trueValue:Int) extends UnaryLocal {
  @inline
  def handleCell(z:Int): Int = { if (cond(z)) trueValue else z } 
} 

object IfCell {
  def apply(r:IntRasterOperation, cond:Int => Boolean,
            trueValue:Int, falseValue: Int) = {
    IfElseCell(r, cond, trueValue, falseValue)
  }

  def apply(r1:IntRasterOperation, r2:IntRasterOperation,
            cond:(Int, Int) => Boolean, trueValue:Int) = {
    new BinaryIfCell(r1, r2, cond, trueValue)
  }

  def apply(r1:IntRasterOperation, r2:IntRasterOperation,
            cond:(Int, Int) => Boolean, trueValue:Int, falseValue:Int) = {
    new BinaryIfElseCell(r1, r2, cond, trueValue, falseValue)
  }
}
