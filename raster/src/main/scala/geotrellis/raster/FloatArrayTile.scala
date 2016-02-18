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
 * ArrayTile based on Array[Float] (each cell as a Float).
 */
abstract class FloatArrayTile(val array: Array[Float], cols: Int, rows: Int)
    extends MutableArrayTile
       with DoubleBasedArrayTile {
  val cellType: FloatCells with NoDataHandling

  def applyDouble(i: Int): Double
  def updateDouble(i: Int, z: Double)

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asFloatBuffer.put(array)
    pixels
  }

  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)
}

final case class FloatRawArrayTile(arr: Array[Float], val cols: Int, val rows: Int)
    extends FloatArrayTile(arr, cols, rows) {
  val cellType = FloatCellType
  def applyDouble(i: Int): Double = arr(i).toDouble
  def updateDouble(i: Int, z: Double) { arr(i) = z.toFloat }
}

final case class FloatConstantNoDataArrayTile(arr: Array[Float], val cols: Int, val rows: Int)
    extends FloatArrayTile(arr, cols, rows) {
  val cellType = FloatConstantNoDataCellType
  def applyDouble(i: Int): Double = arr(i).toDouble
  def updateDouble(i: Int, z: Double) { arr(i) = z.toFloat }
}

final case class FloatUserDefinedNoDataArrayTile(arr: Array[Float], val cols: Int, val rows: Int, val cellType: FloatUserDefinedNoDataCellType)
    extends FloatArrayTile(arr, cols, rows)
       with UserDefinedFloatNoDataConversions {
  val userDefinedFloatNoDataValue = cellType.noDataValue
  def applyDouble(i: Int): Double = udf2d(arr(i))
  def updateDouble(i: Int, z: Double) { arr(i) = d2udf(z) }
}

object FloatArrayTile {
  def apply(arr: Array[Float], cols: Int, rows: Int): FloatArrayTile =
    apply(arr, cols, rows, FloatConstantNoDataCellType)

  def apply(arr: Array[Float], cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(arr, cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(arr, cols, rows)
      case udct @ FloatUserDefinedNoDataCellType(_) =>
        new FloatUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): FloatArrayTile =
    ofDim(cols, rows, FloatConstantNoDataCellType)

  def ofDim(cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(Array.ofDim[Float](cols * rows), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(Array.ofDim[Float](cols * rows), cols, rows)
      case udct @ FloatUserDefinedNoDataCellType(_) =>
        new FloatUserDefinedNoDataArrayTile(Array.ofDim[Float](cols * rows), cols, rows, udct)
    }

  def empty(cols: Int, rows: Int): FloatArrayTile =
    empty(cols, rows, FloatConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(Array.ofDim[Float](cols * rows).fill(Float.NaN), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(Float.NaN), cols, rows)
      case udct @ FloatUserDefinedNoDataCellType(_) =>
        new FloatUserDefinedNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(Float.NaN), cols, rows, udct)
    }

  def fill(v: Float, cols: Int, rows: Int): FloatArrayTile =
    fill(v, cols, rows, FloatConstantNoDataCellType)

  def fill(v: Float, cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows)
      case udct @ FloatUserDefinedNoDataCellType(_) =>
        new FloatUserDefinedNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows, udct)
    }

  private def constructFloatArray(bytes: Array[Byte]): Array[Float] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val floatBuffer = byteBuffer.asFloatBuffer()
    val floatArray = new Array[Float](bytes.size / FloatConstantNoDataCellType.bytes)
    floatBuffer.get(floatArray)
    floatArray
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): FloatArrayTile =
    fromBytes(bytes, cols, rows, FloatConstantNoDataCellType)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(constructFloatArray(bytes), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(constructFloatArray(bytes), cols, rows)
      case udct @ FloatUserDefinedNoDataCellType(_) =>
        new FloatUserDefinedNoDataArrayTile(constructFloatArray(bytes), cols, rows, udct)
    }
}
