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
import geotrellis.raster.io.geotiff.reader.utils.ByteBufferUtils._
import geotrellis.raster.io.geotiff.reader.Tags._

import scala.io._
import java.nio.{ByteBuffer, ByteOrder}

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String)
    extends RuntimeException(msg)

object GeoTiffReader {

  def read(path: String): GeoTiff = read(Filesystem.slurp(path))

  def read(bytes: Array[Byte]): GeoTiff =
    GeoTiffReader(ByteBuffer.wrap(bytes, 0, bytes.size)).read
}

case class GeoTiffReader(byteBuffer: ByteBuffer) {

  val tagReader = TagReader(byteBuffer)

  val imageReader = ImageReader(byteBuffer)

  def read: GeoTiff = {
    setByteBufferPosition
    setByteOrder
    validateTiffVersion
    byteBuffer.position(byteBuffer.getInt)
    GeoTiff(readImageDirectory)
  }

  private def setByteBufferPosition = byteBuffer.position(0)

  private def setByteOrder = (byteBuffer.get.toChar,
    byteBuffer.get.toChar) match {
    case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
    case _ => throw new MalformedGeoTiffException("incorrect byte order")
  }

  private def validateTiffVersion = if (byteBuffer.getChar != 42)
    throw new MalformedGeoTiffException("bad identification number (not 42)")

  private def readImageDirectory: ImageDirectory = {
    val entries = byteBuffer.getShort
    readImageDirectory(ImageDirectory(count = entries), 0)
  }

  private def readImageDirectory(directory: ImageDirectory, index: Int,
    geoKeysMetadata: Option[TagMetadata] = None): ImageDirectory =
    if (index == directory.count) {
      val newDirectory = geoKeysMetadata match {
        case Some(tagMetadata) => tagReader.read(directory, geoKeysMetadata.get)
        case None => directory
      }

      imageReader.read(newDirectory)
    } else {
      val metadata = TagMetadata(byteBuffer.getUnsignedShort,
        byteBuffer.getUnsignedShort, byteBuffer.getInt, byteBuffer.getInt)

      if (metadata.tag == GeoKeyDirectoryTag)
        readImageDirectory(directory, index + 1, Some(metadata))
      else readImageDirectory(tagReader.read(directory, metadata), index + 1,
        geoKeysMetadata)
    }

}
