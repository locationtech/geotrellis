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

package geotrellis.raster.io.geotiff.reader.utils

import java.util.BitSet

object LZWBitInputStream {

  def apply(vector: Vector[Byte]): LZWBitInputStream =
    new LZWBitInputStream(vector)

}

class LZWBitInputStream(vector: Vector[Byte]) {

  private val byteArray = Array.ofDim[Byte](vector.size)

  for (i <- 0 until vector.size) byteArray(i) = ByteInverterUtils.invertByte(vector(i))

  private val bitSet = BitSet.valueOf(byteArray)

  val size = vector.size * 8

  private var index = 0

  def get(next: Int): Int = {
    if (next + index > size) {
      val lastBits = new String((for (i <- index until size) yield (if (bitSet.get(i)) '1' else '0')).toArray)

      throw new IndexOutOfBoundsException(
        s"Index out of bounds for BitInputStream: ${this.toString}. Index was: $index and bitSet size is: $size, last bits are: $lastBits, next is: $next so next + index - size = ${next + index - size}"
      )
    }

    var r0 = 0
    var r1 = 0

    for (i <- 0 until 8)
      if (bitSet.get(i + index))
        r0 |= (1 << (16 - i - 1))

    for (i <- 0 until 8)
      if (bitSet.get(i + 8 + index))
        r1 |= (1 << (8 - i - 1))

    val res = (r0 | r1) >> (16 - next)

    index += next

    res
  }

  def addToIndex(add: Int) = index += add

  def reset = index = 0

  def getIndex(): Int = index

}
