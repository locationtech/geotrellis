/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.index.zcurve

import geotrellis.layers.index.MergeQueue

class Z2(val z: Long) extends AnyVal {
  import Z2._

  def < (other: Z2) = z < other.z
  def > (other: Z2) = z > other.z

  def + (offset: Long) = new Z2(z + offset)
  def - (offset: Long) = new Z2(z - offset)

  def == (other: Z2) = other.z == z

  def decode: (Int, Int) = {
    ( combine(z), combine(z>>1) )
  }

  def dim(i: Int) = Z2.combine(z >> i)

  def mid(p: Z2): Z2 = {
    var ans: Z2  = new Z2(0)
    if (p.z < z)
      ans  = new Z2(p.z + (z - p.z)/2)
    else
      ans = new Z2(z + (p.z - z)/2)
    ans
  }

  def bitsToString = f"(${z.toBinaryString}%16s)(${dim(0).toBinaryString}%8s,${dim(1).toBinaryString}%8s)"
  override def toString = f"$z ${decode}"
}

object Z2 {
  final val MAX_BITS = 31
  final val MAX_MASK = 0x7fffffff // ignore the sign bit, using it breaks < relationship
  final val MAX_DIM = 2

  /** insert 0 between every bit in value. Only first 31 bits can be considred. */
  def split(value: Long): Long = {
    var x: Long = value & MAX_MASK
    x = (x ^ (x << 32)) & 0x00000000ffffffffL
    x = (x ^ (x << 16)) & 0x0000ffff0000ffffL
    x = (x ^ (x <<  8)) & 0x00ff00ff00ff00ffL // 11111111000000001111111100000000..
    x = (x ^ (x <<  4)) & 0x0f0f0f0f0f0f0f0fL // 1111000011110000
    x = (x ^ (x <<  2)) & 0x3333333333333333L // 11001100..
    x = (x ^ (x <<  1)) & 0x5555555555555555L // 1010...
    x
  }

  /** combine every other bit to form a value. Maximum value is 31 bits. */
  def combine(z: Long): Int = {
    var x = z & 0x5555555555555555L;
    x = (x ^ (x >>  1)) & 0x3333333333333333L;
    x = (x ^ (x >>  2)) & 0x0f0f0f0f0f0f0f0fL;
    x = (x ^ (x >>  4)) & 0x00ff00ff00ff00ffL;
    x = (x ^ (x >>  8)) & 0x0000ffff0000ffffL;
    x = (x ^ (x >> 16)) & 0x00000000ffffffffL;
    x.toInt
  }

  /**
   * Bits of x and y will be encoded as ....y1x1y0x0
   */
  def apply(x: Int, y:  Int): Z2 =
    new Z2(split(x) | split(y) << 1)

  def unapply(z: Z2): Option[(Int, Int)] =
    Some(z.decode)

  def zdivide(p: Z2, rmin: Z2, rmax: Z2): (Z2, Z2) = {
    val (litmax,bigmin) = zdiv(load, MAX_DIM)(p.z, rmin.z, rmax.z)
    (new Z2(litmax), new Z2(bigmin))
  }

  /** Loads either 1000... or 0111... into starting at given bit index of a given dimention */
  def load(target: Long, p: Long, bits: Int, dim: Int): Long = {
    val mask = ~(Z2.split(MAX_MASK >> (MAX_BITS-bits)) << dim)
    val wiped = target & mask
    wiped | (split(p) << dim)
  }

  /** Recurse down the quad-tree and report all z-ranges which are contained in the rectangle defined by the min and max points */
  def zranges(min: Z2, max: Z2): Seq[(BigInt, BigInt)] = {
    val mq = new MergeQueue
    val sr = Z2Range(min, max)

    var recCounter = 0
    var reportCounter = 0

    def _zranges(prefix: Long, offset: Int, quad: Long): Unit = {
      recCounter += 1

      val min: Long = prefix | (quad << offset) // QR + 000..
      val max: Long = min | (1L << offset) - 1  // QR + 111..

      val qr = Z2Range(new Z2(min), new Z2(max))
      if (sr contains qr){                         // whole range matches, happy day
        mq += (qr.min.z, qr.max.z)
        reportCounter +=1
      } else if (offset > 0 && (sr overlaps qr)) { // some portion of this range are excluded
        _zranges(min, offset - MAX_DIM, 0)
        _zranges(min, offset - MAX_DIM, 1)
        _zranges(min, offset - MAX_DIM, 2)
        _zranges(min, offset - MAX_DIM, 3)
        //let our children punt on each subrange
      }
    }

    var prefix: Long = 0
    var offset = MAX_BITS*MAX_DIM
    _zranges(prefix, offset, 0) // the entire space
    mq.toSeq
  }
}
