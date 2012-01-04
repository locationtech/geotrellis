package trellis.operation

import trellis.raster.IntRaster
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._


/**
  * Rasterize a polygon and then draw it on the provided raster.
  */
case class BurnPolygon(r:IntRasterOperation,
                       p:PolygonOperation) extends IntRasterOperation with SimpleOperation[IntRaster]{
  def childOperations = List(r, p)
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster  = context.run(CopyRaster(r))
    val polygon = context.run(p)

    Rasterizer.rasterize(raster, Array(polygon))
    raster
  }
}

/**
  * Rasterize a polygon and then draw it on the provided raster.
  */
case class BurnPolygon2(r:IntRasterOperation, p:PolygonOperation,
                        f:Int => Int) extends IntRasterOperation with SimpleOperation[IntRaster]{
  def childOperations = List(r, p)
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster  = context.run(CopyRaster(r))
    val polygon = context.run(p)
    Rasterizer.rasterize(raster, Array(polygon), Array(f))
    raster
  }
}
