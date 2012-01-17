package trellis.operation

import trellis._
import trellis.process._

/**
 * Get the [[trellis.geoattrs.RasterExtent]] from a given raster.
 */
case class GetRasterExtent(r:Op[IntRaster]) extends SimpleOp[RasterExtent] { 
  def _value(context:Context) = context.run(r).rasterExtent
}
