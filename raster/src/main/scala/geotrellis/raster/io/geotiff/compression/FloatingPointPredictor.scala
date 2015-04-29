package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags._
import monocle.syntax._
import spire.syntax.cfor._

/** See TIFF Technical Note 3 */
object FloatingPointPredictor {
  def apply(tags: Tags): FloatingPointPredictor = {
    val colsPerRow = tags.rowSize
    val rowsInSegment: (Int => Int) = { i => tags.rowsInSegment(i) }

    val bandType = tags.bandType

    new FloatingPointPredictor(colsPerRow, rowsInSegment, bandType, tags.bandCount)
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

    // val tmp = bytes.clone

    // cfor(0)(_ < rows, _ + 1) { row =>
    //   // Reorder the float bytes.
    //   val startCol = row * colsPerRow * bandCount
    //   cfor(0)(_ < colsPerRow * bandCount, _ + 1) { col =>
    //     cfor(0)(_ < bytesPerSample, _ + 1) { byteIndex =>
    //       // bytes(bytesPerSample * col + byteIndex) =
    //       //   tmp(byteIndex * colsPerRow + col)
    //       // bytes(bytesPerSample * (col + startCol) + byteIndex) =
    //       //   tmp((bytesPerSample - byteIndex - 1) * colsPerRow + col + startCol)

    //       bytes(bytesPerSample * (col + startCol) + byteIndex) =
    //         tmp(byteIndex * colsPerRow + col + startCol)

    //     }
    //   }
    // }

    bytes
  }
  //       for (BYTE = 0; BYTE < bytesPerSample; ++BYTE)
  //       {
  //         output[ bytes * COL + BYTE ] =
  //           input[ BYTE * rowIncrement + COL ];
  //         #else
  //           output[ bytes * COL + BYTE] =
  //             input[ (bytesPerSample-BYTE-1) * rowIncrement + COL ];
  //         #endif
  //       }
  //     }
  //   }
  // }


  //   val wc = colsPerRow

  //       tmsize_t count = cc;
  //       uint8 *cp = (uint8 *) cp0;
  //       uint8 *tmp = (uint8 *)_TIFFmalloc(cc);

  //       while (count > bandCount) {
  //       	REPEAT4(bandCount, cp[bandCount] += cp[0]; cp++)
  //       	count -= bandCount;
  //       }

  //       _TIFFmemcpy(tmp, cp0, cc);
  //       cp = (uint8 *) cp0;
  //       for (count = 0; count < colsPerRow; count++) {
  //       	uint32 byte;
  //       	for (byte = 0; byte < bytesPerSample; byte++) {
  //       		#if WORDS_BIGENDIAN
  //       		cp[bytesPerSample * count + byte] = tmp[byte * wc + count];
  //       		#else
  //       		cp[bytesPerSample * count + byte] =
  //       			tmp[(bytesPerSample - byte - 1) * wc + count];
  //       		#endif
  //       	}
  //       }
  //       _TIFFfree(tmp);
}
