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

package geotrellis.io.geotiffreader

import scala.io.BufferedSource

object GeoTiffReader {

  def read(stream: BufferedSource): GeoTiff = {
    val streamArray = stream.toArray
    read(streamArray)
  }

  def read(streamArray: Array[Char]): GeoTiff = {
    (streamArray(0), streamArray(1), streamArray(2).toByte) match {
      case ('I', 'I', 42) => readLittleEndianGeoTiff(streamArray)
      case ('M', 'M', 42) => readBigEndianGeoTiff(streamArray)
    }
  }

  private def readLittleEndianGeoTiff(streamArray: Array[Char]): GeoTiff = {
    val offset = GeoTiffReaderUtils.getInt(streamArray, 4)
    println("firstOffset: " + offset)
    val ifds = parseLittleEndianIFDs(streamArray, offset)

    return GeoTiff(ifds)
  }

  private def parseLittleEndianIFDs(streamArray: Array[Char], current: Int):
      List[GeoTiffIFD] = {
    if (current == 0) Nil
    else {
      val entries = GeoTiffReaderUtils.getShort(streamArray, current)
      println("entries: " + entries)
      val fields = Array.ofDim[GeoTiffField](entries)
      parseFields(streamArray, fields, current + 2, 0)
      val offset = GeoTiffReaderUtils.getInt(streamArray,
        entries * 12 + current + 2)
      GeoTiffIFD(fields) :: parseLittleEndianIFDs(streamArray, offset)
    }
  }

  private def parseFields(streamArray: Array[Char], fields: Array[GeoTiffField],
    current: Int, index: Int) {
    if (index != fields.size) {
      val tag = GeoTiffReaderUtils.getShort(streamArray, current)
      println("tag: " + tag)
      val fieldType = GeoTiffReaderUtils.getShort(streamArray, current + 2)
      println("fieldType: " + fieldType)
      val length = GeoTiffReaderUtils.getInt(streamArray, current + 4)
      println("length: " + length)
      val dataOffset = GeoTiffReaderUtils.getInt(streamArray, current + 8)
      println("dataOffset: " + dataOffset)

      val metadata = GTFieldMetadata(tag, fieldType, length, dataOffset)

      val data = GeoTiffFieldDataReader.read(streamArray, metadata)
      fields(index) = GeoTiffField(metadata, data)

      parseFields(streamArray, fields, current + 12, index + 1)
    }
  }

  private def readBigEndianGeoTiff(streamArray: Array[Char]): GeoTiff = ???
}
