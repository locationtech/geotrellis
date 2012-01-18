package trellis.operation

import trellis._
import trellis.process._


/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(re:Op[RasterExtent]) extends SimpleOp[IntRaster] {
  def _value(context:Context) = IntRaster.createEmpty(context.run(re))
}
