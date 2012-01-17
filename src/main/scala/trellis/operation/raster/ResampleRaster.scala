package trellis.operation

import trellis._
import trellis.process._
import trellis.data.IntRasterReader

/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 */
case class ResampleRaster(r:Op[IntRaster], cols:Op[Int], rows:Op[Int]) extends SimpleOp[IntRaster] {
  def _value(context:Context) = {
    val raster = context.run(r)
    val ncols = context.run(cols)
    val nrows = context.run(rows)
    val extent = raster.rasterExtent.extent
    val cw = extent.width / ncols
    val ch = extent.height / nrows
    val re = RasterExtent(extent, cw, ch, ncols, nrows)
    IntRasterReader.read(raster, Option(re))
  }
}
