package trellis.operation

import trellis._
import trellis.process._

/**
 * Negate (multiply by -1) each value in a raster.
 */
case class Negate(r:Op[IntRaster]) extends SimpleUnaryLocal {
  def handleCell(z:Int) = -z
}
