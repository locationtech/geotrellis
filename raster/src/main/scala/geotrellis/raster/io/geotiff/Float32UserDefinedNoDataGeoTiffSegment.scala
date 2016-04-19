package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float32UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedFloatNoDataValue: Float)
    extends Float32GeoTiffSegment(bytes)
       with UserDefinedFloatNoDataConversions {
  def getInt(i: Int): Int = udf2i(get(i))
  def getDouble(i: Int): Double = udf2d(get(i))

  protected def intToFloatOut(v: Int): Float = i2udf(v)
  protected def doubleToFloatOut(v: Double): Float = d2udf(v)
}
