package trellis.operation

import trellis.process._
import trellis.RasterExtent
import trellis.raster.IntRaster
import trellis.data.IntRasterReader

/**
 * This uses a nearest-neighbor algorithm to resample a raster.
 */
case class BurnRaster(r:Op[IntRaster], g:Op[RasterExtent])
extends SimpleOp[IntRaster] {

  def childOperations = List(r, g)

  def _value(context:Context) = {
    val source = context.run(r)
    val geo = context.run(g)

    // this object will read from a raster as a data source
    // (instead of using an arg32/tif/etc) to load a new
    // raster
    IntRasterReader.read(source, Option(geo))
  }
}
