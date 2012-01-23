package trellis.operation

import trellis._
import trellis.process._
import trellis.data.IntRasterReader

/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 */
case class ResampleRaster(r:Op[IntRaster], cols:Op[Int], rows:Op[Int]) 
extends Op3(r,cols,rows)({
  (raster,cols,rows) => {
    val extent = raster.rasterExtent.extent
    val cw = extent.width / cols
    val ch = extent.height / rows
    val re = RasterExtent(extent, cw, ch, cols, rows)
    Result(IntRasterReader.read(raster, Option(re)))
  }
})
