package geotrellis.operation

import geotrellis._
import geotrellis.process._

/**
 * Get the [[geotrellis.geoattrs.RasterExtent]] from a given raster.
 */
case class GetRasterExtent(r:Op[IntRaster]) extends Op1(r) ({
  (r) => Result(r.rasterExtent)
})
