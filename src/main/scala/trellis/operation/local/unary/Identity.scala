package trellis.operation

/**
  * Suspiciously similar to [[trellis.operation.CopyRaster]], Identity returns a new raster with the values of the given raster.
  * TODO: unify with CopyRaster
  */
case class Identity(r:IntRasterOperation) extends UnaryLocal {
  @inline
  def handleCell(z:Int) = { z }
}
