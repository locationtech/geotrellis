package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class UByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Byte)
    extends UByteGeoTiffSegment(bytes)
    with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  def getInt(i: Int): Int = udub2i(getRaw(i))
  def getDouble(i: Int): Double = udub2d(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToUByteOut(v: Double): Byte = d2udb(v)
}
