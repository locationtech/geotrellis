package trellis.operation

import trellis.process._

/**
  * Find the minimum and maximum value of a raster. 
  */
case class FindMinMax(r:IntRasterOperation) extends CachedOperation[(Int,Int)] with SimpleOperation[(Int,Int)] {
  def childOperations = { List(r) }
  override def _value(context:Context) = {
    val raster = context.run(r)
    raster.findMinMax
  }
}
