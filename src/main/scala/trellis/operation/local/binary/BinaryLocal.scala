package trellis.operation

import trellis.raster.IntRaster
import trellis.constant.NODATA
import trellis.process.{Server,Results}
import trellis.raster.IntRaster


/**
  * BinaryLocal is an abstract class for all operations that are both local (operating
  * on each cell in a raster without knowledge of other cells) and binary, by which
  * we mean that the input includes two rasters (as opposed to 'unary' or 'multi').
  */
trait BinaryLocal extends LocalOperation {
  val r1:IntRasterOperation
  val r2:IntRasterOperation

  val identity1:Int
  val identity2:Int

  def getRasters = { Array(r1, r2) }
  def handleCells(z1:Int, z2:Int): Int

  var childStart:Long = 0

  def _run(server:Server, callback:Callback) = {
    val childStart = System.currentTimeMillis()
    runAsync(List(r1, r2), server, callback)
  }

  val nextSteps:Steps = {
    case Results(List(r1:IntRaster, r2:IntRaster)) => step2(r1, r2)
  }

  def step2(raster1:IntRaster, raster2:IntRaster) = {
    childTime = System.currentTimeMillis() - childStart

    val data1  = raster1.data
    val data2  = raster2.data
    val id1    = this.identity1
    val id2    = this.identity2

    val raster3 = raster1.copy()
    val outdata = raster3.data

    var i = 0
    val limit = raster1.length
    while (i < limit) {
      var z1 = data1(i)
      var z2 = data2(i)

      if (z1 != NODATA || z2 != NODATA) {
        if (z1 == NODATA) { z1 = id1 }
        else if (z2 == NODATA) { z2 = id2 }
        outdata(i) = handleCells(z1, z2)
      }
      i += 1
    }
    Some(IntRaster(outdata, raster1.rows, raster1.cols, raster1.rasterExtent.copy))
  }
}
