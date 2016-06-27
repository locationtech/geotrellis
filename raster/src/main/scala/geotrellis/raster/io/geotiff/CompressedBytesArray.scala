package geotrellis.raster.io.geotiff

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import monocle.syntax.apply._
import spire.syntax.cfor._

class CompressedBytesArray(compressedBytes: Array[Array[Byte]]) extends CompressedBytes {

  val size = compressedBytes.size
  def getSegment(i: Int) = compressedBytes(i)
}

object CompressedBytesArray {

  /** Given a ByteBuffer, read through the entire thing and return an
   *  Array[Array[Byte]]
   */
  def apply(byteBuffer: ByteBuffer, storageMethod: StorageMethod,
    tiffTags: TiffTags): CompressedBytesArray = {
      val compressedBytes: Array[Array[Byte]] = {
        def readSections(offsets: Array[Int],
          byteCounts: Array[Int]): Array[Array[Byte]] = {
            val oldOffset = byteBuffer.position

            val result = Array.ofDim[Array[Byte]](offsets.size)

            cfor(0)(_ < offsets.size, _ + 1) { i =>
              byteBuffer.position(offsets(i))
              result(i) = byteBuffer.getSignedByteArray(byteCounts(i))
            }

            byteBuffer.position(oldOffset)

            result
          }

          storageMethod match {
            case _: Striped =>

              val stripOffsets = (tiffTags &|->
                TiffTags._basicTags ^|->
                BasicTags._stripOffsets get)

              val stripByteCounts = (tiffTags &|->
                TiffTags._basicTags ^|->
                BasicTags._stripByteCounts get)

              readSections(stripOffsets.get, stripByteCounts.get)

            case _: Tiled =>
              val tileOffsets = (tiffTags &|->
                TiffTags._tileTags ^|->
                TileTags._tileOffsets get)

              val tileByteCounts = (tiffTags &|->
                TiffTags._tileTags ^|->
                TileTags._tileByteCounts get)

              readSections(tileOffsets.get, tileByteCounts.get)
          }
      }
      new CompressedBytesArray(compressedBytes)
    }
}
