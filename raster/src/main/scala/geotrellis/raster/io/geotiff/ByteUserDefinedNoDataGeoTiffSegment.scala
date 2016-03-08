package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class ByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedByteNoDataValue: Byte)
    extends ByteGeoTiffSegment(bytes)
       with UserDefinedByteNoDataConversions {
  def getInt(i: Int): Int = udb2i(get(i))
  def getDouble(i: Int): Double = udb2d(get(i))

  protected def intToByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToByteOut(v: Double): Byte = d2udb(v)
}
