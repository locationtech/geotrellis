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

object GeoTiffReaderUtils {

  def getShort(streamArray: Array[Char], index: Int) = {
    (streamArray(index + 1) << 8) + streamArray(index)
  }

  def getInt(streamArray: Array[Char], index: Int) =
    (streamArray(index + 3) << 24) + (streamArray(index + 2) <<
      16) + getShort(streamArray, index)

  def getFieldDataArray(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val size = if (metadata.fieldType == 2) 2 else 4
    val array = Array.ofDim[Int](metadata.length / size)

    for (i <- 0 until array.size) {
      val start = metadata.offset + i * size
      if (size == 2) {
        array(i) = GeoTiffReaderUtils.getShort(streamArray, start)
      } else {
        array(i) = GeoTiffReaderUtils.getInt(streamArray, start)
      }
    }

    array
  }

}
