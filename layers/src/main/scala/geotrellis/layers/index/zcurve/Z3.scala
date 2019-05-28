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

class Z3(val z: Long) extends AnyVal {
  import Z3._

  def < (other: Z3) = z < other.z
  def > (other: Z3) = z > other.z
  def >= (other: Z3) = z >= other.z
  def <= (other: Z3) = z <= other.z
  def + (offset: Long) = new Z3(z + offset)
  def - (offset: Long) = new Z3(z - offset)
  def == (other: Z3) = other.z == z

  def decode: (Int, Int, Int) = {
    ( combine(z), combine(z >> 1), combine(z >> 2) )
  }

  def dim(i: Int) = Z3.combine(z >> i)

  def inRange(rmin: Z3, rmax: Z3): Boolean = {
    val (x, y, z) = decode
    x >= rmin.dim(0) &&
    x <= rmax.dim(0) &&
    y >= rmin.dim(1) &&
    y <= rmax.dim(1) &&
    z >= rmin.dim(2) &&
    z <= rmax.dim(2)
  }

  def mid(p: Z3): Z3 = {
    if (p.z < z)
      new Z3(p.z + (z - p.z)/2)
    else
      new Z3(z + (p.z - z)/2)
  }

  def bitsToString = f"(${z.toBinaryString}%16s)(${dim(0).toBinaryString}%8s,${dim(1).toBinaryString}%8s,${dim(2).toBinaryString}%8s)"
  override def toString = f"$z ${decode}"
}

object Z3 {
  final val MAX_BITS = 21
  final val MAX_MASK = 0x1fffffL;
  final val MAX_DIM = 3

  def apply(zvalue: Long) = new Z3(zvalue)

  /** insert 00 between every bit in value. Only first 21 bits can be considred. */
  def split(value: Long): Long = {
    var x = value & MAX_MASK;
    x = (x | x << 32) & 0x1f00000000ffffL
    x = (x | x << 16) & 0x1f0000ff0000ffL
    x = (x | x << 8)  & 0x100f00f00f00f00fL
    x = (x | x << 4)  & 0x10c30c30c30c30c3L
    (x | x << 2)      & 0x1249249249249249L
  }

 /** combine every third bit to form a value. Maximum value is 21 bits. */
  def combine(z: Long): Int = {
    var x = z & 0x1249249249249249L
    x = (x ^ (x >>  2)) & 0x10c30c30c30c30c3L
    x = (x ^ (x >>  4)) & 0x100f00f00f00f00fL
    x = (x ^ (x >>  8)) & 0x1f0000ff0000ffL
    x = (x ^ (x >> 16)) & 0x1f00000000ffffL
    x = (x ^ (x >> 32)) & MAX_MASK
    x.toInt
  }

  /**
   * So this represents the order of the tuple, but the bits will be encoded in reverse order:
   *   ....z1y1x1z0y0x0
   * This is a little confusing.
   */
  def apply(x: Int, y:  Int, z: Int): Z3 = {
    new Z3(split(x) | split(y) << 1 | split(z) << 2)
  }

  def unapply(z: Z3): Option[(Int, Int, Int)] =
    Some(z.decode)

  def zdivide(p: Z3, rmin: Z3, rmax: Z3): (Z3, Z3) = {
    val (litmax,bigmin) = zdiv(load, MAX_DIM)(p.z, rmin.z, rmax.z)
    (new Z3(litmax), new Z3(bigmin))
  }

  /** Loads either 1000... or 0111... into starting at given bit index of a given dimention */
  def load(target: Long, p: Long, bits: Int, dim: Int): Long = {
    val mask = ~(Z3.split(MAX_MASK >> (MAX_BITS-bits)) << dim)
    val wiped = target & mask
    wiped | (split(p) << dim)
  }

  /** Recurse down the oct-tree and report all z-ranges which are contained in the cube defined by the min and max points */
  def zranges(min: Z3, max: Z3): Seq[(BigInt, BigInt)] = {
    var mq: MergeQueue = new MergeQueue
    val sr = Z3Range(min, max)

    var recCounter = 0
    var reportCounter = 0

    def _zranges(prefix: Long, offset: Int, quad: Long): Unit = {
      recCounter += 1

      val min: Long = prefix | (quad << offset) // QR + 000..
      val max: Long = min | (1L << offset) - 1  // QR + 111..
      val qr = Z3Range(new Z3(min), new Z3(max))
      if (sr contains qr){                               // whole range matches, happy day
        mq += (qr.min.z, qr.max.z)
        reportCounter +=1
      } else if (offset > 0 && (sr overlaps qr)) { // some portion of this range are excluded
        _zranges(min, offset - MAX_DIM, 0)
        _zranges(min, offset - MAX_DIM, 1)
        _zranges(min, offset - MAX_DIM, 2)
        _zranges(min, offset - MAX_DIM, 3)
        _zranges(min, offset - MAX_DIM, 4)
        _zranges(min, offset - MAX_DIM, 5)
        _zranges(min, offset - MAX_DIM, 6)
        _zranges(min, offset - MAX_DIM, 7)
        //let our children punt on each subrange
      }
    }

    var prefix: Long = 0
    var offset = MAX_BITS*MAX_DIM
    _zranges(prefix, offset, 0) // the entire space
    mq.toSeq
  }
}
