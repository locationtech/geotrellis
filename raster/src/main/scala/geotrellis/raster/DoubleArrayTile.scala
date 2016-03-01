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
 * ArrayTile based on Array[Double] (each cell as a Double).
 */
abstract class DoubleArrayTile(val array: Array[Double], cols: Int, rows: Int)
    extends MutableArrayTile
       with DoubleBasedArrayTile {
  val cellType: DoubleCells with NoDataHandling

  def applyDouble(i: Int): Double
  def updateDouble(i: Int, z: Double)

  override def toArrayDouble = array.clone

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asDoubleBuffer.put(array)
    pixels
  }
  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)
}

final case class DoubleRawArrayTile(arr: Array[Double], val cols: Int, val rows: Int)
    extends DoubleArrayTile(arr, cols, rows) {
  val cellType = DoubleCellType
  def applyDouble(i: Int): Double = arr(i)
  def updateDouble(i: Int, z: Double) { arr(i) = z.toDouble }
}

final case class DoubleConstantNoDataArrayTile(arr: Array[Double], val cols: Int, val rows: Int)
    extends DoubleArrayTile(arr, cols, rows) {
  val cellType = DoubleConstantNoDataCellType
  def applyDouble(i: Int): Double = arr(i)
  def updateDouble(i: Int, z: Double) { arr(i) = z }
}

final case class DoubleUserDefinedNoDataArrayTile(arr: Array[Double], val cols: Int, val rows: Int, val cellType: DoubleUserDefinedNoDataCellType)
    extends DoubleArrayTile(arr, cols, rows)
       with UserDefinedDoubleNoDataConversions {
  val userDefinedDoubleNoDataValue = cellType.noDataValue
  def applyDouble(i: Int): Double = udd2d(arr(i))
  def updateDouble(i: Int, z: Double) { arr(i) = d2udd(z) }
}

object DoubleArrayTile {
  def apply(arr: Array[Double], cols: Int, rows: Int): DoubleArrayTile =
    apply(arr, cols, rows, DoubleConstantNoDataCellType)

  def apply(arr: Array[Double], cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile =
    cellType match {
      case DoubleCellType =>
        new DoubleRawArrayTile(arr, cols, rows)
      case DoubleConstantNoDataCellType =>
        new DoubleConstantNoDataArrayTile(arr, cols, rows)
      case udct @ DoubleUserDefinedNoDataCellType(_) =>
        new DoubleUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): DoubleArrayTile =
    ofDim(cols, rows, DoubleConstantNoDataCellType)

  def ofDim(cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile =
    cellType match {
      case DoubleCellType =>
        new DoubleRawArrayTile(Array.ofDim[Double](cols * rows), cols, rows)
      case DoubleConstantNoDataCellType =>
        new DoubleConstantNoDataArrayTile(Array.ofDim[Double](cols * rows), cols, rows)
      case udct @ DoubleUserDefinedNoDataCellType(_) =>
        new DoubleUserDefinedNoDataArrayTile(Array.ofDim[Double](cols * rows), cols, rows, udct)
    }

  def empty(cols: Int, rows: Int): DoubleArrayTile =
    empty(cols, rows, DoubleConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      new DoubleRawArrayTile(Array.ofDim[Double](cols * rows).fill(doubleNODATA), cols, rows)
    case DoubleConstantNoDataCellType =>
      new DoubleConstantNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(doubleNODATA), cols, rows)
    case udct @ DoubleUserDefinedNoDataCellType(_) =>
      new DoubleUserDefinedNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(doubleNODATA), cols, rows, udct)
  }

  def fill(v: Double, cols: Int, rows: Int): DoubleArrayTile =
    fill(v, cols, rows, DoubleConstantNoDataCellType)

  def fill(v: Double, cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      new DoubleRawArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)
    case DoubleConstantNoDataCellType =>
      new DoubleConstantNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)
    case udct @ DoubleUserDefinedNoDataCellType(_) =>
      new DoubleUserDefinedNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows, udct)
  }

  private def constructDoubleArray(bytes: Array[Byte]): Array[Double] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val doubleBuffer = byteBuffer.asDoubleBuffer()
    val doubleArray = new Array[Double](bytes.size / DoubleConstantNoDataCellType.bytes)
    doubleBuffer.get(doubleArray)
    doubleArray
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): DoubleArrayTile =
    fromBytes(bytes, cols, rows, DoubleConstantNoDataCellType)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      new DoubleRawArrayTile(constructDoubleArray(bytes), cols, rows)
    case DoubleConstantNoDataCellType =>
      new DoubleConstantNoDataArrayTile(constructDoubleArray(bytes), cols, rows)
    case udct @ DoubleUserDefinedNoDataCellType(_) =>
      new DoubleUserDefinedNoDataArrayTile(constructDoubleArray(bytes), cols, rows, udct)
  }
}
