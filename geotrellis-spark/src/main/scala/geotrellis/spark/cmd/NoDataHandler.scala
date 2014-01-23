package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData

object NoDataHandler {
  def removeUserNodata(rd: MutableRasterData, userNodata: Double): MutableRasterData = {

    def removeInt(rd: MutableRasterData, gtNodata: Int) = {
      var i = 0
      while (i < rd.length) {
        if (rd(i) == userNodata) rd(i) = gtNodata
        i += 1
      }
      rd
    }
    def removeDouble(rd: MutableRasterData, gtNodata: Double) = {
      var i = 0
      while (i < rd.length) {
        if (rd(i) == userNodata) rd.updateDouble(i, gtNodata)
        i += 1
      }
      rd
    }

    val trd = rd.getType match {
      case TypeBit    => removeInt(rd, 0)
      case TypeByte   => removeInt(rd, byteNODATA)
      case TypeShort  => removeInt(rd, shortNODATA)
      case TypeInt    => removeInt(rd, NODATA)
      case TypeFloat  => removeDouble(rd, Float.NaN)
      case TypeDouble => removeDouble(rd, Double.NaN)
    }
    trd
  }

  def removeGtNodata(rd: MutableRasterData, userNodata: Double): RasterData = {
    if (rd.isFloat)
      rd.mapDouble((i: Double) => if (isNoData(i)) userNodata else i)
    else
      rd.map((i: Int) => if (isNoData(i)) userNodata.toInt else i)
  }
}