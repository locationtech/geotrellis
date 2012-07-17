package geotrellis.op.raster.data

import geotrellis.data.RasterReader
import geotrellis.op.{Op,Op2}
import geotrellis.process.Result
import geotrellis.Raster
import geotrellis.RasterExtent

/**
 * Crop and resample a raster to the given raster extent.
 *
 * This may change the geographic extent, and also the grid resolution.
 */
case class WarpRaster(r:Op[Raster], re:Op[RasterExtent]) extends Op2(r, re) ({
  (r, re) => Result(RasterReader.read(r, Option(re)))
})
