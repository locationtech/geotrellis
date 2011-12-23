package trellis.operation

/**
  * Set all values of output raster to one value or another based on whether a condition is true or false.
  */
case class IfElseCell(r:IntRasterOperation, cond:Int => Boolean, trueValue:Int,
                      falseValue:Int) extends UnaryLocal {
  @inline
  def handleCell(z:Int): Int = { if (cond(z)) trueValue else falseValue } 
}
