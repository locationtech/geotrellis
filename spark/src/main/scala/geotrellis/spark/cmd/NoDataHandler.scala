package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData

import scalaxy.loops._

object NoDataHandler {

  def removeUserNoData(rd: MutableRasterData, userNoData: Double): MutableRasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the apply/update methods handle conversion of NODATA to the appropriate types
     * via macros i2f, i2b, respectively 
     */
    for (i <- 0 until rd.length optimized) {
      if (rd(i) == userNoData) rd(i) = NODATA
    }
    rd
  }

  def removeGeotrellisNoData(rd: MutableRasterData, userNoData: Double): RasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the scala will convert the raw types to either Double or Int as per the argument
     * of the anonymous function
     */
    if (rd.isFloat)
      rd.mapDouble((i: Double) => if (isNoData(i)) userNoData else i)
    else
      rd.map((i: Int) => if (isNoData(i)) userNoData.toInt else i)
  }
}