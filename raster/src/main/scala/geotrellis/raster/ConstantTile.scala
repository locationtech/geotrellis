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

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent

import java.nio.ByteBuffer

import spire.syntax.cfor._

trait ConstantTile extends Tile {
  protected val iVal: Int
  protected val dVal: Double

  def get(col: Int, row: Int): Int = iVal
  def getDouble(col: Int, row: Int): Double = dVal

  def toArray(): Array[Int] = Array.ofDim[Int](cols * rows).fill(iVal)
  def toArrayDouble(): Array[Double] = Array.ofDim[Double](cols * rows).fill(dVal)

  def convert(newType: CellType): Tile = 
    newType match {
      case TypeBit => BitConstantTile(if(iVal == 0) false else true, cols, rows)
      case TypeByte => ByteConstantTile(iVal.toByte, cols, rows)
      case TypeShort => ShortConstantTile(iVal.toShort, cols, rows)
      case TypeInt => IntConstantTile(iVal, cols, rows)
      case TypeFloat => FloatConstantTile(dVal.toFloat, cols, rows)
      case TypeDouble => DoubleConstantTile(dVal, cols, rows)
    }

  def foreach(f: Int => Unit) {
    var i = 0
    val len = size
    while (i < len) { f(iVal); i += 1 }
  }

  def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = size
    while (i < len) { f(dVal); i += 1 }
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, iVal)
      }
    }
  }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, dVal)
      }
    }
  }

  def map(f: Int => Int): Tile = IntConstantTile(f(iVal), cols, rows)
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = other.map(z => f(iVal, z))

  def mapDouble(f: Double => Double): Tile = DoubleConstantTile(f(dVal), cols, rows)
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = other.mapDouble(z => f(dVal, z))

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
}

object BitConstantTile { def apply(i: Int, cols: Int, rows: Int): BitConstantTile = BitConstantTile(if(i == 0) false else true, cols, rows) }
case class BitConstantTile(v: Boolean, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = if(v) 1 else NODATA
  protected val dVal = if(v) 1.0 else Double.NaN

  val cellType = TypeBit

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = BitArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = Array(iVal.toByte)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    BitConstantTile(v, target.cols, target.rows)
}

case class ByteConstantTile(v: Byte, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = b2i(v)
  protected val dVal = b2d(v)

  val cellType = TypeByte

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = ByteArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = Array(v)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    ByteConstantTile(v, target.cols, target.rows)
}

case class ShortConstantTile(v: Short, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = s2i(v)
  protected val dVal = s2d(v)

  val cellType = TypeShort

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = ShortArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asShortBuffer.put(v)
    arr
  }

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    ShortConstantTile(v, target.cols, target.rows)
}

case class IntConstantTile(v: Int, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = v
  protected val dVal = i2d(v)

  val cellType = TypeInt

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = IntArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asIntBuffer.put(v)
    arr
  }

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    IntConstantTile(v, target.cols, target.rows)
}

case class FloatConstantTile(v: Float, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = f2i(v)
  protected val dVal = f2d(v)

  val cellType = TypeFloat

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = FloatArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asFloatBuffer.put(v)
    arr
  }

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    FloatConstantTile(v, target.cols, target.rows)
}

case class DoubleConstantTile(v: Double, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = d2i(v)
  protected val dVal = v

  val cellType = TypeDouble

  def toArrayTile(): ArrayTile = mutable

  def mutable(): MutableArrayTile = DoubleArrayTile.fill(v, cols, rows)

  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asDoubleBuffer.put(v)
    arr
  }

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
    DoubleConstantTile(v, target.cols, target.rows)
}
