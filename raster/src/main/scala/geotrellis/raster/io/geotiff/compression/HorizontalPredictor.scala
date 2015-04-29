package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import monocle.syntax._

import java.nio.ByteBuffer
import spire.syntax.cfor._

object HorizontalPredictor {
  def apply(tags: Tags): Predictor = {
    val colsPerRow = tags.rowSize
    val rowsInSegment: (Int => Int) = { i => tags.rowsInSegment(i) }

    val bandType = tags.bandType

    new HorizontalPredictor(colsPerRow, rowsInSegment, tags.bandCount)
      .forBandType(bandType)
  }
}

class HorizontalPredictor(cols: Int, rowsInSegment: Int => Int, bandCount: Int) {
  def forBandType(bandType: BandType): Predictor = {
    val applyFunc: (Array[Byte], Int) => Array[Byte] =
      bandType.bitsPerSample match {
        case  8 => apply8 _
        case 16 => apply16 _
        case 32 => apply32 _
        case _ =>
          throw new MalformedGeoTiffException(s"""Horizontal differencing "Predictor" not supported with ${bandType.bitsPerSample} bits per sample""")
      }

    new Predictor { 
      val checkEndian = true
      def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        applyFunc(bytes, segmentIndex)
    }
  }

  def apply8(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val rows = rowsInSegment(segmentIndex)

    cfor(0)(_ < rows, _ + 1) { row =>
      var count = bandCount * (row * cols + 1)
      cfor(bandCount)(_ < cols * bandCount, _ + 1) { k =>
        bytes(count) = (bytes(count) + bytes(count - bandCount)).toByte
        count += 1
      }
    }

    bytes
  }

  def apply16(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val buffer = ByteBuffer.wrap(bytes).asShortBuffer
    val rows = rowsInSegment(segmentIndex)

    cfor(0)(_ < rows, _ + 1) { row =>
      var count = bandCount * (row * cols + 1)
      cfor(bandCount)(_ < cols * bandCount, _ + 1) { k =>
        buffer.put(count, (buffer.get(count) + buffer.get(count - bandCount)).toShort)
        count += 1
      }
    }

    bytes
  }

  def apply32(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val buffer = ByteBuffer.wrap(bytes).asIntBuffer
    val rows = rowsInSegment(segmentIndex)

    cfor(0)(_ < rows, _ + 1) { row =>
      var count = bandCount * (row * cols + 1)
      cfor(bandCount)(_ < cols * bandCount, _ + 1) { k =>
        buffer.put(count, buffer.get(count) + buffer.get(count - bandCount))
        count += 1
      }
    }

    bytes
  }
}
