package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Int32UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Int)
    extends Int32GeoTiffSegment(bytes)
       with UserDefinedIntNoDataConversions {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = udi2d(get(i))

  protected def intToIntOut(v: Int): Int = i2udi(v)
  protected def doubleToIntOut(v: Double): Int = d2udi(v)
}
