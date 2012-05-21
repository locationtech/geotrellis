package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.data.IntRasterReader

/**
 * Crop and resample a raster to the given raster extent.
 *
 * This may change the geographic extent, and also the grid resolution.
 */
case class WarpRaster(r:Op[IntRaster], re:Op[RasterExtent]) extends Op2(r, re) ({
  (r, re) => Result(IntRasterReader.read(r, Option(re)))
})
