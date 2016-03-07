package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._


class UInt16RawGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
  // we want to preserve Int.MinValue results and this yields a slight performance boost
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUShortOut(v: Int): Short = v.toShort
  protected def doubleToUShortOut(v: Double): Short = v.toShort
}
