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

import ReaderUtils._

object ImageReader {

  def read(streamArray: Array[Char], tags: IFDTags) = {
    if (!tags.basics.stripOffsets.isEmpty) readStripeImage(streamArray, tags)
    else readTileImage(streamArray, tags)
  }

  def readStripeImage(streamArray: Array[Char], tags: IFDTags) = {
    val stripOffsets = tags.basics.stripOffsets.get
    val stripByteCounts = tags.basics.stripByteCounts.get
    val strips = for (i <- 0 until stripOffsets.size) yield {
      streamArray.drop(stripOffsets(i)).take(stripByteCounts(i))
    }

    val imageLength = tags.basics.imageLength.get
    val rowsPerStrip = tags.basics.rowsPerStrip
    val rowByteLength = stripByteCounts(0) / rowsPerStrip

    val rowsUnFlattened = for (i <- 0 until stripOffsets.size) yield {
      val strip = strips(i)
      val mod = imageLength % rowsPerStrip
      val threshold = i == stripOffsets.size - 1 && mod != 0 match {
        case true => mod
        case _ => rowsPerStrip
      }

      for (j <- 0 until threshold) yield {
        strips.drop(j * rowByteLength).take(rowByteLength)
      }
    }

    val rows = rowsUnFlattened.flatten

    tags.basics.compression match {
      case 1 => rows
      case 2 =>
    }
  }

  def readTileImage(streamArray: Array[Char], tags: IFDTags) = {

  }

}
