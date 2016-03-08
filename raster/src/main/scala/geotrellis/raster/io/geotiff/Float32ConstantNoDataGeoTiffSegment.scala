package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float32ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends Float32GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = f2i(get(i))
  def getDouble(i: Int): Double = f2d(get(i))

  protected def intToFloatOut(v: Int): Float = i2f(v)
  protected def doubleToFloatOut(v: Double): Float = d2f(v)
}
