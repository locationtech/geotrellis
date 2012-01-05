package trellis.operation

import trellis.process._
import trellis.raster._

/**
  * Negate (multiply by -1) each value in a raster.
  */
case class Negate(r:IntRasterOperation) extends UnaryLocal {
  //@inline
  //def handleCell(z:Int) = -z
  def getCallback(context:Context) = (z:Int) => -z
}
