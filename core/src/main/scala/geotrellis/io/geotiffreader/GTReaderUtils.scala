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
import scala.collection.mutable.StringBuilder

import java.nio.ByteBuffer

object GTReaderUtils {

  def getShort(streamArray: Array[Char])(index: Int) =
    (streamArray(index + 1) << 8) + streamArray(index)

  def getInt(streamArray: Array[Char])(index: Int) =
    (streamArray(index + 3) << 24) + (streamArray(index + 2) <<
      16) + getShort(streamArray)(index)

  def getDouble(streamArray: Array[Char])(index: Int) = {
    val doubleArray = streamArray.slice(index, index + 8).map(_.toByte).reverse
    ByteBuffer.wrap(doubleArray).getDouble
  }

  def getString(streamArray: Array[Char])(index: Int, length: Int) =
    streamArray.drop(index).take(length - 1).mkString

  def getPartialString(string: String, start: Int, length: Int) =
    string.substring(start, length).mkString.split("\\|")

  def getIntFieldDataArray(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val indicators = metadata.fieldType match {
      case 3 => (2, getShort(streamArray)(_))
      case 4 => (4, getInt(streamArray)(_))
    }

    val array = Array.ofDim[Int](metadata.length)

    fillFieldDataArray[Int](streamArray, metadata, indicators, array)
  }

  def getDoubleFieldDataArray(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val indicators = metadata.fieldType match {
      case 12 => (8, getDouble(streamArray)(_))
    }

    val array = Array.ofDim[Double](metadata.length)

    fillFieldDataArray[Double](streamArray, metadata, indicators, array)
  }

  private def fillFieldDataArray[T : Numeric](streamArray: Array[Char],
    metadata: GTFieldMetadata, indicators: (Int, Int => T),
    array: Array[T]): Array[T] = {
    for (i <- 0 until array.size) {
      val start = metadata.offset + i * indicators._1
      array(i) = indicators._2(start)
    }

    array
  }

}
