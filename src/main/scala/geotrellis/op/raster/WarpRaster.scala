package geotrellis.op.raster

import geotrellis._
import geotrellis.op._
import geotrellis.process._
import geotrellis.data.RasterReader

/**
 * Crop and resample a raster to the given raster extent.
 *
 * This may change the geographic extent, and also the grid resolution.
 */
case class WarpRaster(r:Op[Raster], re:Op[RasterExtent]) extends Op2(r, re) ({
  (r, re) => Result(RasterReader.read(r, Option(re)))
})
