package geotrellis.op.raster

import geotrellis.process._
import geotrellis._
import geotrellis.op._


/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(re:Op[RasterExtent]) extends Op1(re) ({
  (re) => Result(Raster.empty(re))
})
