package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class Int16RawGeoTiffSegment(bytes: Array[Byte]) extends Int16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i).toInt
  def getDouble(i: Int): Double = get(i).toDouble

  protected def intToShortOut(v: Int): Short = v.toShort
  protected def doubleToShortOut(v: Double): Short = v.toShort
}
