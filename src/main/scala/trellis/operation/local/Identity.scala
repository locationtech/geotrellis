package trellis.operation

import trellis.process._
import trellis._

// TODO: unify with CopyRaster

/**
 * Suspiciously similar to [[trellis.operation.CopyRaster]], Identity returns
 * a new raster with the values of the given raster.
 */
case class Identity(r:Op[IntRaster]) extends SimpleUnaryLocal {
  def handleCell(z:Int) = z
}
