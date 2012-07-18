package geotrellis.op.raster.transform

import geotrellis.op.{Op,Op3}
import geotrellis._
import geotrellis.data._
import geotrellis.process.Result



/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 */
case class ResampleRaster(r:Op[Raster], cols:Op[Int], rows:Op[Int]) 
extends Op3(r,cols,rows)({
  (raster,cols,rows) => {
    val extent = raster.rasterExtent.extent
    val cw = extent.width / cols
    val ch = extent.height / rows
    val re = RasterExtent(extent, cw, ch, cols, rows)
    Result(RasterReader.read(raster, Option(re)))
  }
})
