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

//http://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html

package geotrellis.io.geotiff

import ReaderUtils._

import scala.io.BufferedSource

object GeoTiffReader {

  def read(stream: BufferedSource): GeoTiff = {
    val streamArray = stream.toArray
    read(streamArray)
  }

  // Should have the reader in implicit scope!? for byte order.
  def read(streamArray: Array[Char]): GeoTiff = {
    (streamArray(0), streamArray(1), streamArray(2).toByte) match {
      case ('I', 'I', 42) => readLittleEndianGeoTiff(streamArray)
      case ('M', 'M', 42) => readBigEndianGeoTiff(streamArray)
    }
  }

  private def readLittleEndianGeoTiff(streamArray: Array[Char]): GeoTiff = {
    val offset = getInt(streamArray)(4)
    val ifds = parseLittleEndianIFDs(streamArray, offset).toArray

    GeoTiff(ifds)
  }

  private def parseLittleEndianIFDs(streamArray: Array[Char], current: Int):
      List[IFD] = {
    if (current == 0) Nil
    else {
      val entries = getShort(streamArray)(current)
      val tags = parseTags(streamArray, current + 2,
        IFDTags(count = entries), 0, None)

      val image = ImageReader.read(streamArray, tags)

      val offset = getInt(streamArray)(entries * 12 + current + 2)
      IFD(tags) :: parseLittleEndianIFDs(streamArray, offset)
    }
  }

  private def parseTags(streamArray: Array[Char], current: Int,
    tags: IFDTags, index: Int, gkMetadata: Option[TagMetadata]): IFDTags = {
    (tags.geoTiffTags.doubles, tags.geoTiffTags.asciis,
      tags.geoTiffTags.geoKeyDirectory, index, gkMetadata) match {
      case (_, _, _, tags.count, _) => tags
      case (Some(_), Some(_), None, _, Some(metadata)) => parseTags(
        streamArray, current + 12, TagReader.read(streamArray, metadata,
          tags), index + 1, None)
      case _ => {
        val getShortValue = getShort(streamArray)(_)
        val getIntValue = getInt(streamArray)(_)

        val metadata = TagMetadata(getShortValue(current),
          getShortValue(current + 2),
          getIntValue(current + 4),
          getIntValue(current + 8))

        if (metadata.tag == 34735) {
          parseTags(streamArray, current + 12, tags, index,
            Some(metadata))
        } else {
          val newTag = TagReader.read(streamArray, metadata, tags)
          parseTags(streamArray, current + 12, newTag, index + 1,
            gkMetadata)
        }
      }
    }
  }

  private def readBigEndianGeoTiff(streamArray: Array[Char]): GeoTiff = ???
}
