package geotrellis.operation

import geotrellis.process._
import geotrellis._

// TODO: unify with CopyRaster

/**
 * Suspiciously similar to [[geotrellis.operation.CopyRaster]], Identity returns
 * a new raster with the values of the given raster.
 */
case class Identity(r:Op[IntRaster]) extends SimpleUnaryLocal {
  def handleCell(z:Int) = z
}
