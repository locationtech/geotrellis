package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.data._
import geotrellis.raster.op.extent.GetRasterExtent
/**
 * Generate a raster with a new extent and resolution.
 *
 * This uses a nearest-neighbor algorithm to resize a raster.
 *
 * @note               Resample does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Resize(r:Op[Raster], rasterExtent:Op[RasterExtent]) 
	extends Op2(r,rasterExtent) ({
  (raster,rasterExtent) => {
    Result(RasterReader.read(raster, Option(rasterExtent)))
  }
})

object Resize {
  /**
   * Generate a raster with a new extent and resolution. 
   */
  def apply(r:Op[Raster], extentOp:Op[Extent], cols:Op[Int], rows:Op[Int]):Resize = 
    Resize(r,GetRasterExtent(extentOp, cols, rows))

  def apply(r:Op[Raster], cols:Op[Int], rows:Op[Int]):ResizeGrid = ResizeGrid(r, cols, rows)
}

case class ResizeGrid(r:Op[Raster], cols:Op[Int], rows:Op[Int])
extends Op3(r,cols,rows)({
  (raster,cols,rows) => {
    val extent = raster.rasterExtent.extent
    val cw = extent.width / cols
    val ch = extent.height / rows
    val re = RasterExtent(extent, cw, ch, cols, rows)
    Result(RasterReader.read(raster, Option(re)))
  }
})

