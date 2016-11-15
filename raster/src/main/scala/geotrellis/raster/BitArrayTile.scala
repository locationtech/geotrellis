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

package geotrellis.raster


/**
  * [[ArrayTile]] based on an Array[Byte] as a bitmask; values are 0
  * and 1.  Thus, there are 8 boolean (0 / 1) values per byte in the
  * array. For example, Array(11, 9) corresponds to (0 0 0 0 1 0 1 1),
  * (0 0 0 0 1 0 0 1) which means that we have 5 cells set to 1 and 11
  * cells set to 0.
  *
  * Note that unlike the other array-based raster data objects we need
  * to be explicitly told our size, since length=7 and length=8 will
  * both need to allocate an Array[Byte] with length=1.
  */
final case class BitArrayTile(val array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile {
  // i >> 3 is the same as i / 8 but faster
  // i & 7 is the same as i % 8 but faster
  // i & 1 is the same as i % 2 but faster
  // ~3 -> -4, that is 00000011 -> 11111100
  // 3 | 9 -> 11, that is 00000011 | 00001001 -> 00001011
  // 3 & 9 -> 1,  that is 00000011 & 00001001 -> 00000001
  // 3 ^ 9 -> 10, that is 00000011 ^ 00001001 -> 00001010

  if (array.size != (size + 7) / 8) {
    sys.error(s"BitArrayTile array length must be ${(size + 7) / 8}, was ${array.size}")
  }

  val cellType = BitCellType

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def apply(i: Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def update(i: Int, z: Int): Unit =
    BitArrayTile.update(array, i, z)

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum as a double
    */
  def applyDouble(i: Int): Double = i2d(apply(i))

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def updateDouble(i: Int, z: Double): Unit =
    BitArrayTile.updateDouble(array, i, z)

  /**
    * Map each cell in the given raster to a new one, using the given
    * function.
    *
    * @param   f  A function from Int to Int, executed at each point of the [[BitArrayTile]]
    * @return     The result, a [[Tile]]
    */
  override def map(f: Int => Int) = {
    val f0 = f(0) & 1
    val f1 = f(1) & 1

    if (f0 == 0 && f1 == 0) {
      BitConstantTile(false, cols, rows)
    } else if (f0 == 1 && f1 == 1) {
      BitConstantTile(true, cols, rows)
    } else if (f0 == 0 && f1 == 1) {
      // same data as we have now
      this
    } else {
      // inverse (complement) of what we have now
      val clone = array.clone
      var i = 0
      val len = array.size
      while(i < len) { clone(i) = (array(i) ^ -1).toByte ; i += 1 }
      BitArrayTile(clone, cols, rows)
    }
  }

  /**
    * Map each cell in the given raster to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the [[BitArrayTile]]
    * @return     The result, a [[Tile]]
    */
  override def mapDouble(f: Double => Double) = map(z => d2i(f(i2d(z))))

  /**
    * Return a copy of the present [[BitArrayTile]].
    *
    * @return  The copy
    */
  def copy = BitArrayTile(array.clone, cols, rows)

  /**
    * Convert the present [[BitArrayTile]] to an array of bytes and
    * return that array.
    *
    * @return  An array of bytes
    */
  def toBytes: Array[Byte] = array.clone

  def withNoData(noDataValue: Option[Double]): Tile =
    BitArrayTile(array, cols, rows)

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: ByteCells with NoDataHandling =>
        ByteArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * The companion object for the [[BitArrayTile]] type.
  */
object BitArrayTile {

  /**
    * Set or unset the specified bit in the Array[Byte].
    *
    * @param  arr  The array of bytes to be modified
    * @param  i    The index of the bit
    * @param  z    The value whose least significant bit will be put at the index
    */
  def update(arr: Array[Byte], i: Int, z: Int): Unit = {
    val div = i >> 3
    if ((z & 1) == 0) {
      // unset the nth bit
      arr(div) = (arr(div) & ~(1 << (i & 7))).toByte
    } else {
      // set the nth bit
      arr(div) = (arr(div) | (1 << (i & 7))).toByte
    }
  }

  /**
    * Set or unset the specified bit in the Array[Byte].
    *
    * @param  arr  The array of bytes to be modified
    * @param  i    The index of the bit
    * @param  z    The value that will be used to update the array
    */
  def updateDouble(arr: Array[Byte], i: Int, z: Double) : Unit =
    update(arr, i, if(isData(z)) z.toInt else 0)

  /**
    * Produce a [[BitArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new BitArrayTile
    */
  def ofDim(cols: Int, rows: Int): BitArrayTile =
    new BitArrayTile(Array.ofDim[Byte](((cols * rows) + 7) / 8), cols, rows)

  /**
    * Produce an empty, new [[BitArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new BitArrayTile
    */
  def empty(cols: Int, rows: Int): BitArrayTile =
    ofDim(cols, rows)

  /**
    * Produce a new [[BitArrayTile]] and fill it with the given value.
    *
    * @param   v     The value to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new BitArrayTile
    */
  def fill(v: Int, cols: Int, rows: Int): BitArrayTile =
    if(v == 0)
      ofDim(cols, rows)
    else
      new BitArrayTile(Array.ofDim[Byte](((cols * rows) + 7) / 8).fill(1), cols, rows)

  /**
    * Produce a new [[BitArrayTile]] and fill it with the given value.
    *
    * @param   v     The value to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new BitArrayTile
    */
  def fill(v: Boolean, cols: Int, rows: Int): BitArrayTile =
    fill(if(v) 1 else 0, cols, rows)

  /**
    * Produce a new [[BitArrayTile]] from an array of bytes.
    *
    * @param   bytes  The value to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new BitArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): BitArrayTile =
    BitArrayTile(bytes, cols, rows)
}
