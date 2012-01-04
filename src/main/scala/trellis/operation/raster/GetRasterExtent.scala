package trellis.operation

import trellis.RasterExtent
import trellis.process._

/**
  * Get the [[trellis.geoattrs.RasterExtent]] from a given raster.
  */
case class GetRasterExtent(r:IntRasterOperation) extends RasterExtentOperation with SimpleOperation[RasterExtent]{ 
  def childOperations = List(r)

  def _value(context:Context) = context.run(r).rasterExtent

}
