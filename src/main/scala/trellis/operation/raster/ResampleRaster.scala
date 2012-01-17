package trellis.operation

import trellis.data.IntRasterReader
import trellis.RasterExtent
import trellis.process._
import trellis.IntRaster

/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 */
case class ResampleRaster(r:IntRasterOperation, cols:Int, rows:Int)
extends IntRasterOperation with SimpleOperation[IntRaster]{

  def _value(context:Context) = {
    val raster = context.run(r)
    val extent = raster.rasterExtent.extent
    val cw = extent.width / cols
    val ch = extent.height / rows
    val geo = RasterExtent(extent, cw, ch, cols, rows)

    IntRasterReader.read(raster, Option(geo))
  }
}
