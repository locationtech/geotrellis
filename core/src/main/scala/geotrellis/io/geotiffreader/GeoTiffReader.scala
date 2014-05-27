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

  // Should have the reader in implicit scope!? for byte order.
  def read(streamArray: Array[Char]): GeoTiff = {
    (streamArray(0), streamArray(1), streamArray(2).toByte) match {
      case ('I', 'I', 42) => readLittleEndianGeoTiff(streamArray)
      case ('M', 'M', 42) => readBigEndianGeoTiff(streamArray)
    }
  }

  private def readLittleEndianGeoTiff(streamArray: Array[Char]): GeoTiff = {
    val offset = GTReaderUtils.getInt(streamArray)(4)
    val ifds = parseLittleEndianIFDs(streamArray, offset)

    return GeoTiff(ifds.toArray)
  }

  private def parseLittleEndianIFDs(streamArray: Array[Char], current: Int):
      List[GeoTiffIFD] = {
    if (current == 0) Nil
    else {
      val entries = GTReaderUtils.getShort(streamArray)(current)
      println("entries: " + entries)
      val fields = Array.ofDim[GeoTiffField](entries)
      parseFields(streamArray, fields, current + 2, 0, GTDDEmpty())
      val offset = GTReaderUtils.getInt(streamArray)(entries * 12 + current + 2)
      GeoTiffIFD(fields) :: parseLittleEndianIFDs(streamArray, offset)
    }
  }

  private def parseFields(streamArray: Array[Char], fields: Array[GeoTiffField],
    current: Int, index: Int, gtDeps: GTDDependencies) {
    if (index != fields.size) {
      gtDeps match {
        case GTDDComplete(_, _, metadata) => {
          val data = GTFieldDataReader.read(streamArray, metadata, gtDeps)
          fields(index) = GeoTiffField(metadata, data)
          parseFields(streamArray, fields, current + 12, index + 1, GTDDEmpty())
        }
        case _ => {
          val getShort = GTReaderUtils.getShort(streamArray)(_)

          val tag = getShort(current)
          println("tag: " + tag)
          val fieldType = getShort(current + 2)
          println("fieldType: " + fieldType)

          val getInt = GTReaderUtils.getInt(streamArray)(_)
          val length = getInt(current + 4)
          println("length: " + length)
          val dataOffset = getInt(current + 8)
          println("dataOffset: " + dataOffset)

          val metadata = GTFieldMetadata(tag, fieldType, length, dataOffset)

          if (tag == 34735) {
            val newGTDeps = gtDeps.add(metadata)
            parseFields(streamArray, fields, current + 12, index, newGTDeps)
          } else {
            val data = GTFieldDataReader.read(streamArray, metadata)
            fields(index) = GeoTiffField(metadata, data)

            if (tag == 34736 || tag == 34737) {
              val newGTDeps = gtDeps.add(data)
              parseFields(streamArray, fields, current + 12, index + 1,
                newGTDeps)
            } else {
              parseFields(streamArray, fields, current + 12, index + 1, gtDeps)
            }
          }
        }
      }
    }
  }

  private def readBigEndianGeoTiff(streamArray: Array[Char]): GeoTiff = ???
}
