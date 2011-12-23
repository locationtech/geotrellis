package trellis.operation

import trellis.constant.NODATA
import trellis.process.Server
import trellis.stat._


/**
  * Build a histogram from this raster by iterating through each cell value. 
  *
  * See other histogram operations for alternate strategies with better performance.
  */
trait BuildHistogram extends CachedOperation[Histogram] with SimpleOperation[Histogram] {
  val r:IntRasterOperation
  var h:Histogram = null
  def childOperations = { List(r) }
  def initHistogram:Histogram
  def _value(server:Server) = {
    this.h = this.initHistogram

    val raster = server.run(r)
    val data   = raster.data

    var i = 0
    val limit = raster.length
    while (i < limit) {
      val z = data(i)
      if (z == NODATA) {
      } else if (z < 0) {
        println("bad value: " + z)
      } else {
        this.h.countItem(z, 1)
      }
      i += 1
    }
    this.h
  }
}
