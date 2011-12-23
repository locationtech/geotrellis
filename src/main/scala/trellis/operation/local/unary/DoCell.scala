package trellis.operation

/**
  * Perform a function on every cell in a raster.
  *
  * For example,
  * <pre>
  * val R = LoadFile(f)
  * val D = DoCell(R, x => x + 3 ) // add 3 to every cell in the raster  
  * </pre>
  */
case class DoCell(r:IntRasterOperation, localFunction:Int => Int) extends UnaryLocal {
  @inline
  def handleCell(z:Int): Int = localFunction(z)
}
