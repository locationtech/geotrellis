/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags.*
import geotrellis.raster.io.geotiff.reader.{GeoTiffReaderLimitationException, MalformedGeoTiffException}
import java.nio.ByteOrder
import spire.syntax.cfor.*

trait Decompressor extends Serializable {
  def code: Int
  def predictorCode: Int = Predictor.PREDICTOR_NONE

  def byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

  def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte]

  /** Internally, we use the ByteBuffer's default byte ordering (BigEndian).
    * If the decompressed bytes are LittleEndian, flip 'em.
    */
  def flipEndian(bytesPerFlip: Int): Decompressor =
    new Decompressor {
      def code: Int = Decompressor.this.code
      override def predictorCode: Int = Decompressor.this.predictorCode

      override
      def byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN // Since we have to flip, image data is in Little Endian

      def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        flip(Decompressor.this.decompress(bytes, segmentIndex))

      def flip(bytes: Array[Byte]): Array[Byte] = {
        val arr = bytes.clone
        val size = arr.length

        var i = 0
        while (i < size) {
          var j = 0
          while (j < bytesPerFlip) {
            arr(i + j) = bytes(i + bytesPerFlip - 1 - j)
            j += 1
          }

          i += bytesPerFlip
        }

        arr
      }
    }

  def withPredictorDecoding(predictor: Predictor): Decompressor =
    new Decompressor {
      def code: Int = Decompressor.this.code
      override def predictorCode: Int = predictor.code
      override def byteOrder: ByteOrder = Decompressor.this.byteOrder

      def decompress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        predictor.decode(Decompressor.this.decompress(bytes, segmentIndex), segmentIndex)
    }
}

object Decompressor {
  def apply(tiffTags: TiffTags, byteOrder: ByteOrder): Decompressor = {
    import geotrellis.raster.io.geotiff.tags.codes.CompressionType.*

    def checkEndian(d: Decompressor): Decompressor = {
      if(byteOrder != ByteOrder.BIG_ENDIAN && tiffTags.bitsPerPixel() > 8) {
        d.flipEndian(tiffTags.bytesPerPixel / tiffTags.bandCount)
      } else {
        d
      }
    }

    def checkPredictor(d: Decompressor): Decompressor = {
      val predictor = Predictor(tiffTags)
      if(predictor.checkEndian)
        checkEndian(d).withPredictorDecoding(predictor)
      else
        d.withPredictorDecoding(predictor)
    }

    val segmentCount = tiffTags.segmentCount
    val segmentSizes = Array.ofDim[Int](segmentCount)
    val bandCount = tiffTags.bandCount
    if(!tiffTags.hasPixelInterleave || bandCount == 1) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        segmentSizes(i) = tiffTags.imageSegmentByteSize(i).toInt
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        segmentSizes(i) = tiffTags.imageSegmentByteSize(i).toInt * tiffTags.bandCount
      }
    }

    val bits = tiffTags.bitsPerSample
    val packed = bits != 1 && bits != tiffTags.bandType.bitsPerSample
    if (packed) {
      if (tiffTags.hasStripStorage() || (tiffTags.rowSize.toLong * bits) % 8 != 0 ||
          tiffTags.nonBasicTags.fillOrder != 1 ||
          tiffTags.nonBasicTags.predictor.exists(_ != Predictor.PREDICTOR_NONE))
        throw new GeoTiffReaderLimitationException(s"Unsupported $bits-bit TIFF layout or predictor")

      val base = tiffTags.compression match {
        case Uncompressed => NoCompressor
        case LZWCoded => new LZWDecompressor(segmentSizes, strict = true)
        case ZLibCoded | PkZipCoded => new DeflateDecompressor(segmentSizes, strict = true)
        case PackBitsCoded => new PackBitsDecompressor(segmentSizes, strict = true)
        case ZStdCoded => ZStdCompression.createDecompressor
        case compression =>
          throw new GeoTiffReaderLimitationException(s"Unsupported compression $compression for $bits-bit TIFF")
      }
      return new PackedUnsignedDecompressor(base, segmentSizes, bits, tiffTags.bandType.bytesPerSample, byteOrder)
    }

    tiffTags.compression match {
      case Uncompressed =>
        checkEndian(NoCompressor)
      case LZWCoded =>
        checkPredictor(LZWDecompressor(segmentSizes))
      case ZLibCoded | PkZipCoded =>
        checkPredictor(DeflateCompression.createDecompressor(segmentSizes))
      case PackBitsCoded =>
        checkEndian(PackBitsDecompressor(segmentSizes))
      case JpegCoded =>
        checkEndian(JpegDecompressor(tiffTags))
      case ZStdCoded =>
        checkPredictor(ZStdCompression.createDecompressor)

      // Unsupported compression types
      case HuffmanCoded =>
        val msg = "compression type CCITTRLE is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case GroupThreeCoded =>
        val msg = s"compression type CCITTFAX3 is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case GroupFourCoded =>
        val msg = s"compression type CCITTFAX4 is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
      case JpegOldCoded =>
        val msg = "old jpeg (compression = 6) is deprecated."
        throw new MalformedGeoTiffException(msg)
      case compression =>
        val msg = s"compression type $compression is not supported by this reader."
        throw new GeoTiffReaderLimitationException(msg)
    }
  }
}
