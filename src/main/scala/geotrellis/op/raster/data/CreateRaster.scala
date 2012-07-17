package geotrellis.op.raster.data

import geotrellis.op.{Op,Op1}
import geotrellis.process.Result
import geotrellis.Raster
import geotrellis.RasterExtent


/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(re:Op[RasterExtent]) extends Op1(re) ({
  (re) => Result(Raster.empty(re))
})
