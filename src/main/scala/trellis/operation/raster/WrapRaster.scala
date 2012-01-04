package trellis.operation

import trellis.raster.IntRaster
import trellis.process._


/**
  * Return a previously created raster as the product of this operation.
  */
case class WrapRaster(raster:IntRaster) extends IntRasterOperation with SimpleOperation[IntRaster] {
  def childOperations = List.empty[Operation[_]]
  def _value(context:Context) = raster
}

