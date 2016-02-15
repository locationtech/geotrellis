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
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Int] (each cell as an Int).
 */
abstract class IntArrayTile(val array: Array[Int], cols: Int, rows: Int)
    extends MutableArrayTile
       with IntBasedArrayTile {
  val cellType: IntCells with NoDataHandling

  def apply(i: Int): Int
  def update(i: Int, z: Int)

  override def toArray = array.clone

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asIntBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

final case class IntRawArrayTile(arr: Array[Int], val cols: Int, val rows: Int)
    extends IntArrayTile(arr, cols, rows) {
  val cellType = IntCellType
  def apply(i: Int): Int = arr(i)
  def update(i: Int, z: Int) { arr(i) = z }
}

final case class IntConstantNoDataArrayTile(arr: Array[Int], val cols: Int, val rows: Int)
    extends IntArrayTile(arr, cols, rows) {
  val cellType = IntConstantNoDataCellType
  def apply(i: Int): Int = arr(i)
  def update(i: Int, z: Int) { arr(i) = z }
}

final case class IntUserDefinedNoDataArrayTile(arr: Array[Int], val cols: Int, val rows: Int, val cellType: IntUserDefinedNoDataCellType)
    extends IntArrayTile(arr, cols, rows)
       with UserDefinedIntNoDataConversions {
  val userDefinedIntNoDataValue = cellType.noDataValue
  def apply(i: Int): Int = i2udi(arr(i))
  def update(i: Int, z: Int) { arr(i) = udi2i(z) }
}

object IntArrayTile {
  def apply(arr: Array[Int], cols: Int, rows: Int): IntArrayTile =
    apply(arr, cols, rows, IntConstantNoDataCellType)


  def apply(arr: Array[Int], cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(arr, cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(arr, cols, rows)
      case udct @ IntUserDefinedNoDataCellType(_) =>
        new IntUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): IntArrayTile =
    ofDim(cols, rows, IntConstantNoDataCellType)

  def ofDim(cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(Array.ofDim[Int](cols * rows), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(Array.ofDim[Int](cols * rows), cols, rows)
      case udct @ IntUserDefinedNoDataCellType(_) =>
        new IntUserDefinedNoDataArrayTile(Array.ofDim[Int](cols * rows), cols, rows, udct)
    }

  def empty(cols: Int, rows: Int): IntArrayTile =
    empty(cols, rows, IntConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(Array.ofDim[Int](cols * rows).fill(NODATA), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(NODATA), cols, rows)
      case udct @ IntUserDefinedNoDataCellType(_) =>
        new IntUserDefinedNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(NODATA), cols, rows, udct)
    }

  def fill(v: Int, cols: Int, rows: Int): IntArrayTile =
    fill(v, cols, rows, IntConstantNoDataCellType)

  def fill(v: Int, cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
      case udct @ IntUserDefinedNoDataCellType(_) =>
        new IntUserDefinedNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows, udct)
    }

  private def constructIntArray(bytes: Array[Byte]): Array[Int] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](bytes.size / IntConstantNoDataCellType.bytes)
    intBuffer.get(intArray)
    intArray
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): IntArrayTile =
    fromBytes(bytes, cols, rows, IntConstantNoDataCellType)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(constructIntArray(bytes), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(constructIntArray(bytes), cols, rows)
      case udct @ IntUserDefinedNoDataCellType(_) =>
        new IntUserDefinedNoDataArrayTile(constructIntArray(bytes), cols, rows, udct)
    }
}
