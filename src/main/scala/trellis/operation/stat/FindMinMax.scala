package trellis.operation

import trellis.process.Server

/**
  * Find the minimum and maximum value of a raster. 
  */
case class FindMinMax(r:IntRasterOperation) extends CachedOperation[(Int,Int)] with SimpleOperation[(Int,Int)] {
  def childOperations = { List(r) }
  override def _value(server:Server) = {
    val raster = server.run(r)
    raster.findMinMax
  }
}
