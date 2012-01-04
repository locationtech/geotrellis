package trellis.operation

import trellis.RasterExtent
import trellis.process._
import trellis.raster.IntRaster

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */

case class LoadFile(p:Op[String]) extends SimpleOp[IntRaster] {
  def childOperations = List(p)

  def _value(context:Context) = {
    val path = context.run(p)
    context.loadRaster(path, null)
  }
}

case class LoadFileWithRasterExtent(p:Op[String], e:Op[RasterExtent]) extends SimpleOp[IntRaster] {
  def childOperations = List(p, e)
  
  def _value(context:Context) = {
    val path = context.run(p)
    val rasterExtent = context.run(e)
    context.loadRaster(path, rasterExtent)
  }
}

object LoadFile {
  def apply(p:Op[String], e:Op[RasterExtent]) = LoadFileWithRasterExtent(p, e)
}
