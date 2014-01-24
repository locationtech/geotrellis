package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData

object NoDataHandler {

  def removeUserNodata(rd: MutableRasterData, userNodata: Double): MutableRasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the apply/update methods handle conversion of NODATA to the appropriate types
     * via macros i2f, i2b, respectively 
     */
    var i = 0
    while (i < rd.length) {
      if (rd(i) == userNodata) rd(i) = NODATA
      i += 1
    }
    rd
  }

  def removeGtNodata(rd: MutableRasterData, userNodata: Double): RasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the scala will convert the raw types to either Double or Int as per the argument
     * of the anonymous function
     */
    if (rd.isFloat)
      rd.mapDouble((i: Double) => if (isNoData(i)) userNodata else i)
    else
      rd.map((i: Int) => if (isNoData(i)) userNodata.toInt else i)
  }
}