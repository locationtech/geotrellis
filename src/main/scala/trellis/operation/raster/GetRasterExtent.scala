package trellis.operation

import trellis._
import trellis.process._

/**
 * Get the [[trellis.geoattrs.RasterExtent]] from a given raster.
 */
case class GetRasterExtent(r:Op[IntRaster]) extends Op1(r) ({
  (r) => Result(r.rasterExtent)
})
