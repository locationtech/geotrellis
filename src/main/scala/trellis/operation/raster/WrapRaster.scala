package trellis.operation

import trellis.raster.IntRaster
import trellis.process.Server


/**
  * Return a previously created raster as the product of this operation.
  */
case class WrapRaster(raster:IntRaster) extends IntRasterOperation with SimpleOperation[IntRaster] {
  def childOperations = List.empty[Operation[_]]
  def _value(server:Server) = raster
}

