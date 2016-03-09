package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Int32RawGeoTiffSegment(bytes: Array[Byte]) extends Int32GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = i2d(get(i))

  protected def intToIntOut(v: Int): Int = v
  protected def doubleToIntOut(v: Double): Int = d2i(v)
}
