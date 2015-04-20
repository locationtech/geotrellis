/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.tags._

import monocle.syntax._

import scala.io._
import scala.collection.mutable
import java.nio.{ByteBuffer, ByteOrder}
import spire.syntax.cfor._

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

// To implement for tile
//
//    val cellType: geotrellis.raster.CellType = ???
//    val rows: Int = ???
//    val cols: Int = ???
//
//    def get(col: Int,row: Int): Int = ???
//    def getDouble(col: Int,row: Int): Double = ???

//    def combine(r2: geotrellis.raster.Tile)(f: (Int, Int) => Int): geotrellis.raster.Tile = ???
//    def combineDouble(r2: geotrellis.raster.Tile)(f: (Double, Double) => Double): geotrellis.raster.Tile = ???
//    def map(f: Int => Int): geotrellis.raster.Tile = ???
//    def mapDouble(f: Double => Double): geotrellis.raster.Tile = ???
//    def resample(source: geotrellis.vector.Extent,target: geotrellis.raster.RasterExtent,method: geotrellis.raster.interpolation.InterpolationMethod): geotrellis.raster.Tile = ???
//
//    def mutable: geotrellis.raster.MutableArrayTile = ???
//    def toArray(): Array[Int] = ???
//    def toArrayDouble(): Array[Double] = ???
//    def toArrayTile(): geotrellis.raster.ArrayTile = ???
//    def toBytes(): Array[Byte] = ???


object GeoTiffReader {

  def read(path: String): GeoTiff = read(Filesystem.slurp(path))

  def read(bytes: Array[Byte]): GeoTiff = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate GeoTiff identification number
    val geoTiffIdNumber = byteBuffer.getChar
    if ( geoTiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $geoTiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    val tiffTags = TagsReader.read(byteBuffer, tagsStartPosition)

    val compressedSections: Array[Array[Byte]] = {
      def readSections(offsets: Array[Int], byteCounts: Array[Int]): Array[Array[Byte]] = {
        val oldOffset = byteBuffer.position

        val result = Array.ofDim[Array[Byte]](offsets.size)

        cfor(0)(_ < offsets.size, _ + 1) { i =>
          byteBuffer.position(offsets(i))
          result(i) = byteBuffer.getSignedByteArray(byteCounts(i))
        }

        byteBuffer.position(oldOffset)

        result
      }

      def readStrips(tags: Tags): Array[Array[Byte]] = {
        val stripOffsets = (tags &|->
          Tags._basicTags ^|->
          BasicTags._stripOffsets get)

        val stripByteCounts = (tags &|->
          Tags._basicTags ^|->
          BasicTags._stripByteCounts get)

        readSections(stripOffsets.get, stripByteCounts.get)
      }

      def readTiles(tags: Tags) = {
        val tileOffsets = (tags &|->
          Tags._tileTags ^|->
          TileTags._tileOffsets get)

        val tileByteCounts = (tags &|->
          Tags._tileTags ^|->
          TileTags._tileByteCounts get)

        readSections(tileOffsets.get, tileByteCounts.get)
      }

      if (tiffTags.hasStripStorage) readStrips(tiffTags)
      else readTiles(tiffTags)
    }



    import geotrellis.raster.io.geotiff.tags.codes.CompressionType._
    import geotrellis.raster.io.geotiff.reader.decompression._

    val uncompressedSections: Array[Array[Byte]] =
      tiffTags.compression match {
        case Uncompressed => compressedSections
        case LZWCoded => compressedSections.uncompressLZW(tiffTags)
        case ZLibCoded | PkZipCoded => compressedSections.uncompressZLib(tiffTags)
        case PackBitsCoded => compressedSections.uncompressPackBits(tiffTags)

        // Unsupported compression types
        case JpegCoded => 
          val msg = "compression type JPEG is not supported by this reader."
          throw new GeoTiffReaderLimitationException(msg)
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

    val imageBytes = ImageConverter(tiffTags,
      byteBuffer.order == ByteOrder.BIG_ENDIAN).convert(uncompressedSections)


    val metaData = tiffTags.metaData
    val tags = tiffTags.tags
    val bandTags = tiffTags.bandTags

    val bands: Seq[Tile] = {
      val cols = tiffTags.cols
      val rows = tiffTags.rows
      val bandCount = tiffTags.bandCount

      val tileBuffer = mutable.ListBuffer[Tile]()
      val tileSize = metaData.cellType.numBytes(cols * rows)

      cfor(0)(_ < bandCount, _ + 1) { i =>
        val arr = Array.ofDim[Byte](tileSize)
        System.arraycopy(imageBytes, i * tileSize, arr, 0, tileSize)

        tileBuffer += {
          (tiffTags &|-> Tags._geoTiffTags
            ^|-> GeoTiffTags._gdalInternalNoData get) match {
            case Some(gdalNoData) =>
              ArrayTile.fromBytes(arr, metaData.cellType, cols, rows, gdalNoData)
            case None =>
              ArrayTile.fromBytes(arr, metaData.cellType, cols, rows)
          }
        }
      }

      tileBuffer.toList
    }

    val geoTiffBands =
      for ((band, tags) <- bands.zip(bandTags)) yield GeoTiffBand(band, metaData.rasterExtent.extent, metaData.crs, tags)

    GeoTiff(metaData, geoTiffBands, tags, tiffTags)
  }

  /////////////////////////////////////////////////////////
  def readNew(path: String): GeoTiffTile = readNew(Filesystem.slurp(path))

  def readNew(bytes: Array[Byte]): GeoTiffTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate GeoTiff identification number
    val geoTiffIdNumber = byteBuffer.getChar
    if ( geoTiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $geoTiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    val tiffTags = TagsReader.read(byteBuffer, tagsStartPosition)

    import geotrellis.raster.io.geotiff.tags.codes.CompressionType._
    import geotrellis.raster.io.geotiff.reader.decompression._

    val decompressor: Decompressor = {
      val d = 
        tiffTags.compression match {
          case Uncompressed => NoCompression
          case LZWCoded => LZWDecompressor(tiffTags)
          case ZLibCoded | PkZipCoded => ZLibCompression.createDecompressor(tiffTags)
          case PackBitsCoded => PackBitsDecompressor(tiffTags)

          // Unsupported compression types
          case JpegCoded =>
            val msg = "compression type JPEG is not supported by this reader."
            throw new GeoTiffReaderLimitationException(msg)
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

      // If we need to flip the byte order, do at decompression time.
      if(byteBuffer.order != ByteOrder.BIG_ENDIAN && tiffTags.bitsPerPixel > 8) {
        d.flipEndian(tiffTags.bytesPerPixel)
      } else {
        d
      }
    }

    val compressedBytes: Array[Array[Byte]] = {
      def readSections(offsets: Array[Int], byteCounts: Array[Int]): Array[Array[Byte]] = {
        val oldOffset = byteBuffer.position

        val result = Array.ofDim[Array[Byte]](offsets.size)

        cfor(0)(_ < offsets.size, _ + 1) { i =>
          byteBuffer.position(offsets(i))
          result(i) = byteBuffer.getSignedByteArray(byteCounts(i))
        }

        byteBuffer.position(oldOffset)

        result
      }

      def readStrips(tags: Tags): Array[Array[Byte]] = {
        val stripOffsets = (tags &|->
          Tags._basicTags ^|->
          BasicTags._stripOffsets get)

        val stripByteCounts = (tags &|->
          Tags._basicTags ^|->
          BasicTags._stripByteCounts get)

        readSections(stripOffsets.get, stripByteCounts.get)
      }

      def readTiles(tags: Tags) = {
        val tileOffsets = (tags &|->
          Tags._tileTags ^|->
          TileTags._tileOffsets get)

        val tileByteCounts = (tags &|->
          Tags._tileTags ^|->
          TileTags._tileByteCounts get)

        readSections(tileOffsets.get, tileByteCounts.get)
      }

      if (tiffTags.hasStripStorage) 
        readStrips(tiffTags)
      else 
        readTiles(tiffTags)

    }
    
    val layout = GeoTiffSegmentLayout(tiffTags)
    val noDataValue = 
      (tiffTags
        &|-> Tags._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get)

    // If the GeoTiff is coming is as uncompressed, leave it as uncompressed.
    // If it's any sort of compression, move forward with ZLib compression.
    val compression =
      decompressor match {
        case NoCompression => NoCompression
        case _ => ZLibCompression
      }

    GeoTiffTile(tiffTags.metaData.bandType, compressedBytes, decompressor, layout, compression, noDataValue)
  }
}
