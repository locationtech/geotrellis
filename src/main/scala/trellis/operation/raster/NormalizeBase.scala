package trellis.operation

import trellis.raster.IntRaster
import trellis.constant.NODATA
import trellis.process._


/**
  * Abstract class for other normalize operations.
  */
trait NormalizeBase extends IntRasterOperation with SimpleOperation[IntRaster] {
  val r:IntRasterOperation
  val gmin:Int
  val gmax:Int

  override def childOperations = List(r)

  def getMinMax(raster:IntRaster):(Int, Int)

  def _value(server:Server)(implicit t:Timer):IntRaster = {
    val raster:IntRaster = server.run(r)

    val (zmin, zmax) = this.getMinMax(raster)

    // in this case there was no data in the raster, so we can't really
    // normalize it... just return
    if (zmin > zmax) {
      return raster
    }

    val grange = gmax - gmin
    val zrange = zmax - zmin
    val data   = raster.data

    var i = 0
    val limit = raster.length
    while (i < limit) {
      val z:Int = data(i)
      if (z != NODATA) {
        data(i) = (z - zmin) * grange / zrange + gmin
      }
      i += 1
    }

    raster
  }
}
