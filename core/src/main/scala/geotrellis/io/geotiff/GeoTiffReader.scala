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

package geotrellis.io.geotiff

import scala.io.BufferedSource

import java.nio.{ByteBuffer, ByteOrder}

import geotrellis.io.geotiff.utils.ByteBufferUtils._

class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

object GeoTiffReader {

  def apply(source: BufferedSource): GeoTiffReader =
    GeoTiffReader(ByteBuffer.wrap(source.map(_.toByte).toArray))

}

case class GeoTiffReader(byteBuffer: ByteBuffer) {

  val tagReader = TagReader(byteBuffer)

  val imageReader = ImageReader(byteBuffer)

  def read(): GeoTiff = {
    setByteBufferPosition
    setByteOrder
    validateTiffVersion
    byteBuffer.position(byteBuffer.getInt)
    GeoTiff(readImageDirectories.toVector)
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

  private def readImageDirectories: List[ImageDirectory] =
    byteBuffer.position match {
      case 0 => Nil
      case _ => {
        val current = byteBuffer.position
        val entries = byteBuffer.getShort
        val directory = readImageDirectory(ImageDirectory(count = entries), 0)
        byteBuffer.position(entries * 12 + current + 2).position(
          byteBuffer.getInt)
        directory :: readImageDirectories
      }
    }

  private def readImageDirectory(directory: ImageDirectory, index: Int,
    geoKeysMetadata: Option[TagMetadata] = None): ImageDirectory =
    if (index == directory.count) geoKeysMetadata match {
      case Some(tagMetadata) => {
        val newDirectory = tagReader.read(directory, geoKeysMetadata.get)
        imageReader.read(newDirectory)
      }
      case None => throw new MalformedGeoTiffException("no geokey tag")
    } else {
      val metadata = TagMetadata(byteBuffer.getUnsignedShort,
        byteBuffer.getUnsignedShort, byteBuffer.getInt, byteBuffer.getInt)

      if (metadata.tag == 34735) readImageDirectory(directory, index + 1,
        Some(metadata))
      else readImageDirectory(tagReader.read(directory, metadata), index + 1,
        geoKeysMetadata)
    }

}
