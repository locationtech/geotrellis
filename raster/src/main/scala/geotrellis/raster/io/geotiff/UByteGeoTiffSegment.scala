package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

abstract class UByteGeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  val size: Int = bytes.size

  def get(i: Int): Int = bytes(i) & 0xFF
  def getRaw(i: Int): Byte = bytes(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def intToUByteOut(v: Int): Byte
  protected def doubleToUByteOut(v: Double): Byte

  def map(f: Int => Int): Array[Byte] = {
    val arr = bytes.clone
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToUByteOut(f(getInt(i)))
    }
    arr
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => d2i(f(i2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToUByteOut(f(i, getInt(i)))
    }
    arr
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = doubleToUByteOut(f(i, getDouble(i)))
    }
    arr
  }
}
