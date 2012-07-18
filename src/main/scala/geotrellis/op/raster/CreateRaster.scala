package geotrellis.op.raster

import geotrellis._
import geotrellis.op._
import geotrellis.process._

/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(re:Op[RasterExtent]) extends Op1(re) ({
  (re) => Result(Raster.empty(re))
})
