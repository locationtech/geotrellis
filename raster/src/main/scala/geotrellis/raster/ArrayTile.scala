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

package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._

object ArrayTile {
  def alloc(t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case TypeBit    => BitArrayTile.ofDim(cols, rows)
      case TypeByte   => ByteArrayTile.ofDim(cols, rows)
      case TypeUByte  => UByteArrayTile.ofDim(cols, rows)
      case TypeShort  => ShortArrayTile.ofDim(cols, rows)
      case TypeUShort  => UShortArrayTile.ofDim(cols, rows)
      case TypeInt    => IntArrayTile.ofDim(cols, rows)
      case TypeFloat  => FloatArrayTile.ofDim(cols, rows)
      case TypeDouble => DoubleArrayTile.ofDim(cols, rows)
    }

  def empty(t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case TypeBit    => BitArrayTile.empty(cols, rows)
      case TypeByte   => ByteArrayTile.empty(cols, rows)
      case TypeUByte  => UByteArrayTile.empty(cols, rows)
      case TypeShort  => ShortArrayTile.empty(cols, rows)
      case TypeUShort  => UShortArrayTile.empty(cols, rows)
      case TypeInt    => IntArrayTile.empty(cols, rows)
      case TypeFloat  => FloatArrayTile.empty(cols, rows)
      case TypeDouble => DoubleArrayTile.empty(cols, rows)
    }

  def fromBytes(bytes: Array[Byte], t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case TypeBit    => BitArrayTile.fromBytes(bytes, cols, rows)
      case TypeByte   => ByteArrayTile.fromBytes(bytes, cols, rows)
      case TypeUByte  => UByteArrayTile.fromBytes(bytes, cols, rows)
      case TypeShort  => ShortArrayTile.fromBytes(bytes, cols, rows)
      case TypeUShort  => UShortArrayTile.fromBytes(bytes, cols, rows)
      case TypeInt    => IntArrayTile.fromBytes(bytes, cols, rows)
      case TypeFloat  => FloatArrayTile.fromBytes(bytes, cols, rows)
      case TypeDouble => DoubleArrayTile.fromBytes(bytes, cols, rows)
    }

  def fromBytes(bytes: Array[Byte], t: CellType, cols: Int, rows: Int, replaceNoData: Double): MutableArrayTile =
    t match {
      case TypeBit    => BitArrayTile.fromBytes(bytes, cols, rows, if(replaceNoData == 0) 0 else 1)
      case TypeByte   => ByteArrayTile.fromBytes(bytes, cols, rows, replaceNoData.toByte)
      case TypeUByte  => UByteArrayTile.fromBytes(bytes, cols, rows)
      case TypeShort  => ShortArrayTile.fromBytes(bytes, cols, rows, replaceNoData.toShort)
      case TypeUShort  => UShortArrayTile.fromBytes(bytes, cols, rows)
      case TypeInt    => IntArrayTile.fromBytes(bytes, cols, rows, replaceNoData.toInt)
      case TypeFloat  => FloatArrayTile.fromBytes(bytes, cols, rows, replaceNoData.toFloat)
      case TypeDouble => DoubleArrayTile.fromBytes(bytes, cols, rows, replaceNoData)
    }

  def apply(arr: Array[Byte], cols: Int, rows: Int) = ByteArrayTile(arr, cols, rows)
  def apply(arr: Array[Short], cols: Int, rows: Int) = ShortArrayTile(arr, cols, rows)
  def apply(arr: Array[Int], cols: Int, rows: Int) = IntArrayTile(arr, cols, rows)
  def apply(arr: Array[Float], cols: Int, rows: Int) = FloatArrayTile(arr, cols, rows)
  def apply(arr: Array[Double], cols: Int, rows: Int) = DoubleArrayTile(arr, cols, rows)
}

/**
 * ArrayTile provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait ArrayTile extends Tile with Serializable {
  def toArrayTile = this

  def convert(cellType: CellType): Tile =
    LazyConvertedArrayTile(this, cellType)

  def foreach(f: Int => Unit): Unit = {
    val len = size
    var i = 0
    while (i < len) {
      f(apply(i))
      i += 1
    }
  }

  def foreachDouble(f: Double => Unit): Unit = {
    val len = size
    var i = 0
    while (i < len) {
      f(applyDouble(i))
      i += 1
    }
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, get(col, row))
      }
    }
  }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, getDouble(col, row))
      }
    }
  }

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def map(f: Int=>Int): Tile = {
    val output = ArrayTile.alloc(cellType, cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output(i) = f(apply(i))
      i += 1
    }
    output
  }

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def mapDouble(f: Double => Double): Tile = {
    val len = size
    val tile = ArrayTile.alloc(cellType, cols, rows)
    var i = 0
    while (i < len) {
      tile.updateDouble(i, f(applyDouble(i)))
      i += 1
    }
    tile
  }

  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, mapper(col, row, get(col, row)))
      }
    }
    tile
  }

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, mapper(col, row, getDouble(col, row)))
      }
    }
    tile
  }

  /**
   * Combine two ArrayTile's cells into new cells using the given integer
   * function. For every (x, y) cell coordinate, get each ArrayTile's integer
   * value, map them to a new value, and assign it to the output's (x, y) cell.
   */
  def combine(other: ArrayTile)(f: (Int, Int) => Int): ArrayTile = {
    (this, other).assertEqualDimensions

    val output = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output(i) = f(apply(i), other(i))
      i += 1
    }
    output
  }

  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    other match {
      case ar: ArrayTile =>
        combine(ar)(f)
      case ct: ConstantTile =>
        ct.combine(this)(f)
      case ct: CompositeTile =>
        ct.combine(this)((z1, z2)=>f(z2, z1))
    }
  }

  /**
   * Combine two ArrayTile's cells into new cells using the given double
   * function. For every (x, y) cell coordinate, get each ArrayTile's double
   * value, map them to a new value, and assign it to the output's (x, y) cell.
   */
  def combineDouble(other: ArrayTile)(f: (Double, Double) => Double): ArrayTile = {
    (this, other).assertEqualDimensions

    val output = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
      i += 1
    }
    output
  }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    other match {
      case ar: ArrayTile =>
        combineDouble(ar)(f)
      case ct: ConstantTile =>
        ct.combineDouble(this)(f)
      case ct: CompositeTile =>
        ct.combineDouble(this)((z1, z2) => f(z2, z1))
    }
  }

  override def equals(other: Any): Boolean = other match {
    case r: ArrayTile => {
      if (r == null) return false
      val len = size
      if (len != r.size) return false
      var i = 0
      while (i < len) {
        if (apply(i) != r(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }

  def apply(i: Int): Int
  def applyDouble(i: Int): Double

  def get(col: Int, row: Int) = apply(row * cols + col)
  def getDouble(col: Int, row: Int) = applyDouble(row * cols + col)

  def copy: ArrayTile

  def toList = toArray.toList
  def toListDouble = toArrayDouble.toList

  def toArray: Array[Int] = {
    val len = size
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = apply(i)
      i += 1
    }
    arr
  }

  def toArrayDouble: Array[Double] = {
    val len = size
    val arr = Array.ofDim[Double](len)
    var i = 0
    while (i < len) {
      arr(i) = applyDouble(i)
      i += 1
    }
    arr
  }

  def toBytes: Array[Byte]
}
