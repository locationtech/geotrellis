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

package geotrellis.io.geotiff.utils

import java.util.BitSet

object BitSetUtils {

  implicit class ByteBufferUtilities(bitSet: BitSet) {

    def toByteVector(size: Int): Vector[Byte] = {
      val bytes = Array.ofDim[Byte]((size + 7) / 8)

      for (i <- 0 until size)
        if (bitSet.get(i))
          bytes(i / 8) = (bytes(i / 8).toInt | (1 << (i % 8))).toByte

      bytes.toVector
    }

  }

}
