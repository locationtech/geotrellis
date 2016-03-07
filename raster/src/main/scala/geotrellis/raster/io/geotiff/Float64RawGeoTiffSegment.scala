package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float64RawGeoTiffSegment(bytes: Array[Byte]) extends Float64GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i).toInt
  def getDouble(i: Int): Double = get(i)

  protected def intToDoubleOut(v: Int): Double = v.toDouble
  protected def doubleToDoubleOut(v: Double): Double = v
}
