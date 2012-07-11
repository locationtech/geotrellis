package geotrellis.op.raster

import geotrellis._
import geotrellis.process._
import geotrellis._
import geotrellis.op._

/**
 * Get the [[geotrellis.geoattrs.RasterExtent]] from a given raster.
 */
case class GetRasterExtent(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.rasterExtent)
})
