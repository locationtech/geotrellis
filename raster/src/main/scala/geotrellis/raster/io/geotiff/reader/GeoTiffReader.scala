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

import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff.reader.Tags._

import scala.io._
import java.nio.{ByteBuffer, ByteOrder}

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

object GeoTiffReader {

  def read(path: String): GeoTiff = read(Filesystem.slurp(path))

  def read(bytes: Array[Byte]): GeoTiff = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    def validateTiffVersion = 

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

    byteBuffer.position(byteBuffer.getInt)

    val entryCount = byteBuffer.getShort

    val imageDirectory = {

      def recurReadImageDirectory(directory: ImageDirectory, index: Int, geoKeysMetadata: Option[TagMetadata] = None): ImageDirectory =

        if (index == entryCount) {
          val newDirectory =
            geoKeysMetadata match {
              case Some(tagMetadata) => TagReader.read(byteBuffer, directory, geoKeysMetadata.get)
              case None => directory
            }
          ImageReader.read(byteBuffer, newDirectory)
        } else {
          val metadata =
            TagMetadata(
              byteBuffer.getUnsignedShort,
              byteBuffer.getUnsignedShort,
              byteBuffer.getInt,
              byteBuffer.getInt
            )

          if (metadata.tag == GeoKeyDirectoryTag)
            recurReadImageDirectory(directory, index + 1, Some(metadata))
          else
            recurReadImageDirectory(
              TagReader.read(byteBuffer, directory, metadata),
              index + 1,
              geoKeysMetadata
            )
        }

      val directory = ImageDirectory()
      recurReadImageDirectory(directory, 0)
    }

    val metaData = imageDirectory.metaData
    val bands = imageDirectory.bands
    val tags = imageDirectory.tags
    val bandTags = imageDirectory.bandTags

    val geoTiffBands =
      for ((band, tags) <- bands.zip(bandTags)) yield GeoTiffBand(band, metaData.rasterExtent.extent, metaData.crs, tags)

    GeoTiff(metaData, geoTiffBands, tags, imageDirectory)
  }
}
