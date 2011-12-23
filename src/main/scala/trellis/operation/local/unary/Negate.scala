package trellis.operation

/**
  * Negate (multiply by -1) each value in a raster.
  */
case class Negate(r:IntRasterOperation) extends UnaryLocal {
  @inline
  def handleCell(z:Int) = -z
}
