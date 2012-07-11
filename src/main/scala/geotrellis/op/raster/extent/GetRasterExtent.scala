package geotrellis.op.raster.extent

import geotrellis.op._
import geotrellis.process.Result
import geotrellis.Raster

/**
 * Get the [[geotrellis.geoattrs.RasterExtent]] from a given raster.
 */
case class GetRasterExtent(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.rasterExtent)
})
