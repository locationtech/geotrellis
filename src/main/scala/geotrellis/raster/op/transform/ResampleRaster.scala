package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.data._

/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 *
 * @note               Resample does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
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
