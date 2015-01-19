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

package geotrellis.raster.io.geotiff.reader.extensions

import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

trait ArrayExtensions {

  implicit class ArrayUtilities(arr: Array[Byte]) {

    def readIntNumber(byteSize: Int, index: Int): Int = {
      if (byteSize != 1 && byteSize != 2 && byteSize != 4)
        throw new IllegalArgumentException(s"bad byteSize, can only be 1, 2 or 4, was $byteSize")

      val start = index * byteSize
      val end = start + byteSize

      var int = 0

      cfor(start)(_ < end, _ + 1) { i =>
        int += arr(i) << i * 8
      }

      int
    }

    def readFloatPointNumber(byteSize: Int, index: Int): Double = {
      if (byteSize != 4 && byteSize != 8)
        throw new IllegalArgumentException(s"bad byteSize, can only be 4 or 8, was $byteSize")

      val start = index * byteSize
      val end = start + byteSize

      val bb = ByteBuffer.allocate(byteSize).order(ByteOrder.LITTLE_ENDIAN)

      cfor(start)(_ < end, _ + 1) { i =>
        bb.put(arr(i))
      }

      bb.position(0)
      if (byteSize == 4) bb.getFloat.toDouble
      else bb.getDouble
    }

  }

}
