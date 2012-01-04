package trellis.operation

import trellis.RasterExtent
import trellis.process._

/**
  * Get the [[trellis.geoattrs.RasterExtent]] from a given raster.
  */
case class GetRasterExtent(r:IntRasterOperation) extends SimpleOperation[RasterExtent]{ 
  def _value(context:Context) = context.run(r).rasterExtent

}
