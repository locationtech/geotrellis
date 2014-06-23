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

import spire.syntax.cfor._

object RasterData {
  def largestType(lhs: RasterData, rhs: RasterData) = {
    lhs.getType.union(rhs.getType)
  }
  def largestByType(lhs: RasterData, rhs: RasterData) = {
    if (largestType(lhs, rhs) == lhs.getType) lhs else rhs
  }
  def largestAlloc(lhs: RasterData, rhs: RasterData, cols: Int, rows: Int) = {
    largestByType(lhs, rhs).alloc(cols, rows)
  }

  def allocByType(t: RasterType, cols: Int, rows: Int): MutableRasterData = t match {
    case TypeBit    => BitArrayRasterData.ofDim(cols, rows)
    case TypeByte   => ByteArrayRasterData.ofDim(cols, rows)
    case TypeShort  => ShortArrayRasterData.ofDim(cols, rows)
    case TypeInt    => IntArrayRasterData.ofDim(cols, rows)
    case TypeFloat  => FloatArrayRasterData.ofDim(cols, rows)
    case TypeDouble => DoubleArrayRasterData.ofDim(cols, rows)
  }

  def emptyByType(t: RasterType, cols: Int, rows: Int): MutableRasterData = t match {
    case TypeBit    => BitArrayRasterData.empty(cols, rows)
    case TypeByte   => ByteArrayRasterData.empty(cols, rows)
    case TypeShort  => ShortArrayRasterData.empty(cols, rows)
    case TypeInt    => IntArrayRasterData.empty(cols, rows)
    case TypeFloat  => FloatArrayRasterData.empty(cols, rows)
    case TypeDouble => DoubleArrayRasterData.empty(cols, rows)
  }

  def fromArrayByte(bytes: Array[Byte], awType: RasterType, cols: Int, rows: Int) = awType match {
    case TypeBit    => BitArrayRasterData.fromArrayByte(bytes, cols, rows)
    case TypeByte   => ByteArrayRasterData.fromArrayByte(bytes, cols, rows)
    case TypeShort  => ShortArrayRasterData.fromArrayByte(bytes, cols, rows)
    case TypeInt    => IntArrayRasterData.fromArrayByte(bytes, cols, rows)
    case TypeFloat  => FloatArrayRasterData.fromArrayByte(bytes, cols, rows)
    case TypeDouble => DoubleArrayRasterData.fromArrayByte(bytes, cols, rows)
  }

  def apply(arr: Array[Byte], cols: Int, rows: Int) = ByteArrayRasterData(arr,cols,rows)
  def apply(arr: Array[Short], cols: Int, rows: Int) = ShortArrayRasterData(arr,cols,rows)
  def apply(arr: Array[Int], cols: Int, rows: Int) = IntArrayRasterData(arr,cols,rows)
  def apply(arr: Array[Float], cols: Int, rows: Int) = FloatArrayRasterData(arr,cols,rows)
  def apply(arr: Array[Double], cols: Int, rows: Int) = DoubleArrayRasterData(arr,cols,rows)
}

/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait RasterData extends Serializable {
  def force: RasterData
  def getType: RasterType
  def alloc(cols: Int, rows: Int): MutableRasterData

  def isFloat = getType.float
  def convert(typ: RasterType): RasterData = LazyConvert(this, typ)
  def lengthLong = length

  def isLazy: Boolean = false

  def copy: RasterData
  def length: Int

  def cols: Int
  def rows: Int

  def mutable(): MutableRasterData

  /**
   * For every cell in the given raster, run the given integer function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreach(f: Int => Unit): Unit = {
    var i = 0
    val len = length
    while (i < len) {
      f(apply(i))
      i += 1
    }
  }

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def map(f:Int=>Int):RasterData = {
    val output = alloc(cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(apply(i))
      i += 1
    }
    output
  }
//  def map(f:Int=>Int):RasterData = LazyMap(this,f)

  /**
   * Combine two RasterData's cells into new cells using the given integer
   * function. For every (x,y) cell coordinate, get each RasterData's integer
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combine(other: RasterData)(f: (Int, Int) => Int): RasterData = {
    if (lengthLong != other.lengthLong) {
      val size1 = s"${cols} x ${rows}"
      val size2 = s"${other.cols} x ${other.rows}"
      sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
    }
    val output = RasterData.largestAlloc(this, other, cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(apply(i), other(i))
      i += 1
    }
    output
  }
  // def combine(other:RasterData)(f:(Int,Int) => Int):RasterData =
  //   LazyCombine(this,other,f)

  /**
   * For every cell in the given raster, run the given double function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreachDouble(f: Double => Unit): Unit = {
    var i = 0
    val len = length
    while (i < len) {
      f(applyDouble(i))
      i += 1
    }
  }

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def mapDouble(f:Double => Double):RasterData = {
    val len = length
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      data.updateDouble(i, f(applyDouble(i)))
      i += 1
    }
    data
  }
//  def mapDouble(f:Double => Double):RasterData = LazyMapDouble(this, f)

  /**
   * Combine two RasterData's cells into new cells using the given double
   * function. For every (x,y) cell coordinate, get each RasterData's double
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combineDouble(other: RasterData)(f: (Double, Double) => Double): RasterData = {
    if (lengthLong != other.lengthLong) {
      val size1 = s"${cols} x ${rows}"
      val size2 = s"${other.cols} x ${other.rows}"
      sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
    }
    val output = RasterData.largestAlloc(this, other, cols, rows)
    var i = 0
    val len = length
    while (i < len) {
      output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
      i += 1
    }
    output
  }
  // def combineDouble(other: RasterData)(f: (Double, Double) => Double): RasterData =
  //   LazyCombineDouble(this, other, f)

  override def equals(other: Any): Boolean = other match {
    case r: RasterData => {
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

  def warp(current:RasterExtent,target:RasterExtent):RasterData 
}
