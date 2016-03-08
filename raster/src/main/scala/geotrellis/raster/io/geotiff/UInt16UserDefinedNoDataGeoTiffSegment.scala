package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._


class UInt16UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedShortNoDataValue: Short)
    extends UInt16GeoTiffSegment(bytes)
       with UserDefinedShortNoDataConversions {

  def getInt(i: Int): Int = uds2i(getRaw(i))
  def getDouble(i: Int): Double = uds2d(getRaw(i))

  protected def intToUShortOut(v: Int): Short = i2uds(v)
  protected def doubleToUShortOut(v: Double): Short = d2uds(v)
}
