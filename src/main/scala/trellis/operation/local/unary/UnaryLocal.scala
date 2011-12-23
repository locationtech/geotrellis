package trellis.operation

import trellis.constant.NODATA
import trellis.process.{Server,Results}
import trellis.raster.IntRaster

import scala.math.{max, min, pow}

/**
  * Abstract class for all operations that are unary (operate on a single raster) and
  * are local (operate on each cell without knowledge of other cells).
  */
trait UnaryLocal extends LocalOperation with SimpleOperation[IntRaster] {
  val r:IntRasterOperation

  def handleCell(z:Int): Int

  def getRasters = Array(r)

  def _value(server:Server) = {
    val childStart = System.currentTimeMillis
    val raster = server.run(r)
    childTime = System.currentTimeMillis - childStart
    val data   = raster.data
    var i = 0
    val limit = raster.length
    while (i < limit) {
      val z = data(i)
      if (z != NODATA) {
        data(i) = handleCell(z)
      }
      i += 1
    }
    raster
  }
}
