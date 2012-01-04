package trellis.operation

import trellis.operation

import scala.math.{min, max}
import scala.util.Sorting
import trellis.RasterExtent
import trellis.raster.IntRaster
import trellis.process._
import trellis.constant.NODATA

/**
 * Creates an empty raster object based on the given raster properties.
 */
case class CreateRaster(g:RasterExtentOperation) extends SimpleOperation[IntRaster] {
  def _value(context:Context) = { IntRaster.createEmpty( context.run(g) ) }
  
}
