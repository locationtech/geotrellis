package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class Int16ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends Int16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = s2i(get(i))
  def getDouble(i: Int): Double = s2d(get(i))

  protected def intToShortOut(v: Int): Short = i2s(v)
  protected def doubleToShortOut(v: Double): Short = d2s(v)
}
