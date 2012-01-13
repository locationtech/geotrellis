package trellis.operation

import trellis.raster._
import trellis.process._

/**
  * Find the minimum and maximum value of a raster. 
  */
case class FindMinMax(r:Op[IntRaster]) extends SimpleOp[(Int, Int)] with CachedOp[(Int, Int)] {
  def _value(context:Context) = context.run(r).findMinMax
}
