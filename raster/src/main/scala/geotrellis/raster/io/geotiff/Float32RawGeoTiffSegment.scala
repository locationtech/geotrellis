package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float32RawGeoTiffSegment(bytes: Array[Byte]) extends Float32GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i).toInt
  def getDouble(i: Int): Double = get(i).toDouble

  protected def intToFloatOut(v: Int): Float = v.toFloat
  protected def doubleToFloatOut(v: Double): Float = v.toFloat
}
