package trellis.operation

import trellis.process._
import trellis._

/**
 * Negate (multiply by -1) each value in a raster.
 */
case class Negate(r:IntRasterOperation) extends UnaryLocal {
  def getCallback(context:Context) = (z:Int) => -z
}
