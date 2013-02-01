package geotrellis.raster.op.extent

import geotrellis._
import geotrellis.rest.op.string.ParseInt

/**
 * Get the [[RasterExtent]] from a given raster.
 */
case class GetRasterExtentFromRaster(r:Op[Raster]) extends Op1(r) ({
  (r) => Result(r.rasterExtent)
})

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class GetRasterExtent(extent:Op[Extent], cols:Op[Int], rows:Op[Int])
  extends Op3 (extent,cols,rows) ({
  (e, cols, rows) => 
    val cw = (e.xmax - e.xmin) / cols
    val ch = (e.ymax - e.ymin) / rows
    Result(RasterExtent(e, cw, ch, cols, rows))
})

object GetRasterExtent {
  def apply(xmin:Double, ymin:Double, xmax:Double, ymax:Double, cols:Int, rows:Int):GetRasterExtent = {
    GetRasterExtent(Extent(xmin,ymin,xmax,ymax),cols,rows)
  }
  def apply(r:Op[Raster]) = GetRasterExtentFromRaster(r)
}
