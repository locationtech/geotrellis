package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class Int16UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedShortNoDataValue: Short)
    extends Int16GeoTiffSegment(bytes)
       with UserDefinedShortNoDataConversions {
  def getInt(i: Int): Int = uds2i(get(i))
  def getDouble(i: Int): Double = uds2d(get(i))

  protected def intToShortOut(v: Int): Short = i2uds(v)
  protected def doubleToShortOut(v: Double): Short = d2uds(v)
}
