package trellis.operation

import trellis.process._
import trellis.raster._

/**
  * Suspiciously similar to [[trellis.operation.CopyRaster]], Identity returns a new raster with the values of the given raster.
  * TODO: unify with CopyRaster
  */
case class Identity(r:Operation[IntRaster]) extends UnaryLocal {
  //@inline
  //def handleCell(z:Int) = { z }
  def getCallback(context:Context) = (z:Int) => z
}
