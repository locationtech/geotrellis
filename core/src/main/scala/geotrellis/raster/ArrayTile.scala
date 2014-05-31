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

import geotrellis._
import geotrellis.feature.Extent

import scalaxy.loops._

object ArrayTile {
  def largestType(lhs: ArrayTile, rhs: ArrayTile) = {
    lhs.getType.union(rhs.getType)
  }

  def largestByType(lhs: ArrayTile, rhs: ArrayTile) = {
    if (largestType(lhs, rhs) == lhs.getType) lhs else rhs
  }

  def largestAlloc(lhs: ArrayTile, rhs: ArrayTile, cols: Int, rows: Int) = {
    largestByType(lhs, rhs).alloc(cols, rows)
  }

  def allocByType(t: RasterType, cols: Int, rows: Int): MutableArrayTile = t match {
    case TypeBit    => BitArrayTile.ofDim(cols, rows)
    case TypeByte   => ByteArrayTile.ofDim(cols, rows)
    case TypeShort  => ShortArrayTile.ofDim(cols, rows)
    case TypeInt    => IntArrayTile.ofDim(cols, rows)
    case TypeFloat  => FloatArrayTile.ofDim(cols, rows)
    case TypeDouble => DoubleArrayTile.ofDim(cols, rows)
  }

  def emptyByType(t: RasterType, cols: Int, rows: Int): MutableArrayTile = t match {
    case TypeBit    => BitArrayTile.empty(cols, rows)
    case TypeByte   => ByteArrayTile.empty(cols, rows)
    case TypeShort  => ShortArrayTile.empty(cols, rows)
    case TypeInt    => IntArrayTile.empty(cols, rows)
    case TypeFloat  => FloatArrayTile.empty(cols, rows)
    case TypeDouble => DoubleArrayTile.empty(cols, rows)
  }

  def fromArrayByte(bytes: Array[Byte], awType: RasterType, cols: Int, rows: Int) = awType match {
    case TypeBit    => BitArrayTile.fromArrayByte(bytes, cols, rows)
    case TypeByte   => ByteArrayTile.fromArrayByte(bytes, cols, rows)
    case TypeShort  => ShortArrayTile.fromArrayByte(bytes, cols, rows)
    case TypeInt    => IntArrayTile.fromArrayByte(bytes, cols, rows)
    case TypeFloat  => FloatArrayTile.fromArrayByte(bytes, cols, rows)
    case TypeDouble => DoubleArrayTile.fromArrayByte(bytes, cols, rows)
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
trait ArrayTile extends Raster with Serialization {
  def getType: RasterType
  def alloc(cols: Int, rows: Int): MutableArrayTile

  def convert(typ: RasterType): ArrayTile = LazyConvertedTile(this, typ)
  def lengthLong = length

  def isLazy: Boolean = false

  def copy: ArrayTile
  def length: Int

  def mutable(): MutableArrayTile

  def toArrayTile = ArrayTile(this, cols, rows)
  val rasterType = getType
  def data = this

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def map(f: Int=>Int): ArrayTile = {
    val output = alloc(cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(apply(i))
      i += 1
    }
    output
  }

  /**
   * Combine two ArrayTile's cells into new cells using the given integer
   * function. For every (x, y) cell coordinate, get each ArrayTile's integer
   * value, map them to a new value, and assign it to the output's (x, y) cell.
   */
  def combine(other: ArrayTile)(f: (Int, Int) => Int): ArrayTile = {
    if (lengthLong != other.lengthLong) {
      val size1 = s"${cols} x ${rows}"
      val size2 = s"${other.cols} x ${other.rows}"
      sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
    }
    val output = ArrayTile.largestAlloc(this, other, cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(apply(i), other(i))
      i += 1
    }
    output
  }

  def combine(r2: Raster)(f: (Int, Int) => Int): Raster = {
    if(this.dimensions != r2.dimensions) {
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
                             s"$dimensions does not match ${r2.dimensions}")
    }
    r2 match {
      case ar: ArrayTile => 
        Raster(data.combine(ar.data)(f), cols, rows)
      case tr: TileRaster =>
        tr.combine(this)((z1, z2)=>f(z2, z1))
    }
  }


  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def mapDouble(f: Double => Double): ArrayTile = {
    val len = length
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      data.updateDouble(i, f(applyDouble(i)))
      i += 1
    }
    data
  }

  /**
   * Combine two ArrayTile's cells into new cells using the given double
   * function. For every (x, y) cell coordinate, get each ArrayTile's double
   * value, map them to a new value, and assign it to the output's (x, y) cell.
   */
  def combineDouble(other: ArrayTile)(f: (Double, Double) => Double): ArrayTile = {
    if (lengthLong != other.lengthLong) {
      val size1 = s"${cols} x ${rows}"
      val size2 = s"${other.cols} x ${other.rows}"
      sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
    }
    val output = ArrayTile.largestAlloc(this, other, cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
      i += 1
    }
    output
  }

  def combineDouble(r2: Raster)(f: (Double, Double) => Double): Raster = {
    if(this.dimensions != r2.dimensions) {
      throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
                             s"$dimensions does not match ${r2.dimensions}")
    }
    r2 match {
      case ar: ArrayTile => 
        Raster(data.combineDouble(ar.data)(f), cols, rows)
      case cr: CroppedRaster =>
        cr.combineDouble(this)((z1, z2) => f(z2, z1))
    }
  }

  override def equals(other: Any): Boolean = other match {
    case r: ArrayTile => {
      if (r == null) return false
      val len = length
      if (len != r.length) return false
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

  def toList = toArray.toList
  def toListDouble = toArrayDouble.toList

  def toArray: Array[Int] = {
    val len = length
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = apply(i)
      i += 1
    }
    arr
  }

  def toArrayDouble: Array[Double] = {
    val len = length
    val arr = Array.ofDim[Double](len)
    var i = 0
    while (i < len) {
      arr(i) = applyDouble(i)
      i += 1
    }
    arr
  }

  def toArrayByte: Array[Byte]

  def warp(current: Extent, target: RasterExtent): ArrayTile 
}
