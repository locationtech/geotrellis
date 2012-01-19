package trellis.operation

import trellis._
import trellis.process._


/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(re:Op[RasterExtent]) extends Op1(re) ({
  (re) => Result(IntRaster.createEmpty(re))
})
