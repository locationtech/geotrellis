package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class UInt16ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = us2i(getRaw(i))
  def getDouble(i: Int): Double = us2d(getRaw(i))

  protected def intToUShortOut(v: Int): Short = i2us(v)
  protected def doubleToUShortOut(v: Double): Short = d2us(v)
}
