/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster

import geotrellis._

import scalaxy.loops._

/**
 * RasterData based on an Array[Byte] as a bitmask; values are 0 and 1.
 * Thus, there are 8 boolean (0/1) values per byte in the array. For example,
 * Array(11, 9) corresponds to (0 0 0 0 1 0 1 1), (0 0 0 0 1 0 0 1) which
 * means that we have 5 cells set to 1 and 11 cells set to 0.
 *
 * Note that unlike the other array-based raster data objects we need to be
 * explicitly told our size, since length=7 and length=8 will both need to
 * allocate an Array[Byte] with length=1.
 */
final case class BitArrayRasterData(array: Array[Byte], cols: Int, rows: Int)
  extends MutableRasterData with IntBasedArray {
  val size = cols * rows

  // i >> 3 is the same as i / 8 but faster
  // i & 7 is the same as i % 8 but faster
  // i & 1 is the same as i % 2 but faster
  // ~3 -> -4, that is 00000011 -> 11111100
  // 3 | 9 -> 11, that is 00000011 | 00001001 -> 00001011
  // 3 & 9 -> 1,  that is 00000011 & 00001001 -> 00000001
  // 3 ^ 9 -> 10, that is 00000011 ^ 00001001 -> 00001010

  if (array.length != (size + 7) / 8) {
    sys.error(s"BitArrayRasterData array length must be ${(size + 7) / 8}, was ${array.length}")
  }
  def getType = TypeBit
  def alloc(cols: Int, rows: Int) = BitArrayRasterData.ofDim(cols, rows)
  def length = size
  def apply(i: Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]
  def update(i: Int, z: Int): Unit = {
    val div = i >> 3
    if ((z & 1) == 0) {
      // unset the nth bit
      array(div) = (array(div) & ~(1 << (i & 7))).toByte
    } else {
      // set the nth bit
      array(div) = (array(div) | (1 << (i & 7))).toByte
    }
  }
  def copy = BitArrayRasterData(array.clone, cols, rows)

  override def map(f: Int => Int) = {
    val f0 = f(0) & 1
    val f1 = f(1) & 1

    if (f0 == 0 && f1 == 0) {
      BitConstant(false, cols, rows)
    } else if (f0 == 1 && f1 == 1) {
      BitConstant(true, cols, rows)
    } else if (f0 == 0 && f1 == 1) {
      // same data as we have now
      this
    } else {
      // inverse (complement) of what we have now
      val clone = array.clone
      var i = 0
      val len = array.length
      while(i < len) { clone(i) = (array(i) ^ -1).toByte ; i += 1 }
      BitArrayRasterData(clone, cols, rows)
    }
  }

  override def mapDouble(f: Double => Double) = map(z => d2i(f(i2d(z))))

  def toArrayByte: Array[Byte] = array

  def warp(current:RasterExtent,target:RasterExtent):RasterData = {
    val warped = Array.ofDim[Byte]((target.cols*target.rows+7)/8).fill(byteNODATA)
    Warp(current,target,new BitWarpAssign(array,warped))
    BitArrayRasterData(warped, target.cols, target.rows)
  }
}

object BitArrayRasterData {
  def ofDim(cols: Int, rows: Int) = new BitArrayRasterData(Array.ofDim[Byte](((cols * rows) + 7) / 8), cols, rows)
  def empty(cols: Int, rows: Int) = ofDim(cols, rows)

  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int) = BitArrayRasterData(bytes, cols, rows)
}
