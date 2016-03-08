package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class ByteConstantNoDataCellTypeGeoTiffSegment(bytes: Array[Byte]) extends ByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  protected def intToByteOut(v: Int): Byte = i2b(v)
  protected def doubleToByteOut(v: Double): Byte = d2b(v)
}
