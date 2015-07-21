package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import monocle.syntax._
import spire.syntax.cfor._

/** See TIFF Technical Note 3 */
object FloatingPointPredictor {
  def apply(tiffTags: TiffTags): FloatingPointPredictor = {
    val colsPerRow = tiffTags.rowSize
    val rowsInSegment: (Int => Int) = { i => tiffTags.rowsInSegment(i) }

    val bandType = tiffTags.bandType

    new FloatingPointPredictor(colsPerRow, rowsInSegment, bandType, tiffTags.bandCount)
  }
}

class FloatingPointPredictor(colsPerRow: Int, rowsInSegment: Int => Int, bandType: BandType, bandCount: Int) extends Predictor {
  val checkEndian = false

  def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val rows = rowsInSegment(segmentIndex)
    val stride = bandCount
    val bytesPerSample = bandType.bytesPerSample

    val colValuesPerRow = colsPerRow * bandCount
    val bytesPerRow = colValuesPerRow * bytesPerSample

    cfor(0)(_ < rows, _ + 1) { row =>
      // Undo the byte differencing
      val rowByteIndex = row * bytesPerRow

      val limit = (row + 1) * bytesPerRow
      cfor(rowByteIndex + bandCount)(_ < limit, _ + 1) { i =>
        bytes(i) = (bytes(i) + bytes(i - bandCount)).toByte
      }

      val tmp = Array.ofDim[Byte](bytesPerRow)
      System.arraycopy(bytes, rowByteIndex, tmp, 0, bytesPerRow)

      cfor(0)(_ < colsPerRow * bandCount, _ + 1) { col =>
        cfor(0)(_ < bytesPerSample, _ + 1) { byteIndex =>
          bytes(rowByteIndex + (bytesPerSample * col + byteIndex)) =
            tmp(byteIndex * colsPerRow + col)
        }
      }
    }
    bytes
  }
}
