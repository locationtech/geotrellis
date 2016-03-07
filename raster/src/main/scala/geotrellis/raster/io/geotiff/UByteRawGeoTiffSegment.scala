package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class UByteRawGeoTiffSegment(bytes: Array[Byte]) extends UByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUByteOut(v: Int): Byte = v.toByte
  protected def doubleToUByteOut(v: Double): Byte = v.toByte
}
