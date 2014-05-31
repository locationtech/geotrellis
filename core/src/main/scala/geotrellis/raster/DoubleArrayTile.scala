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

import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Double] (each cell as a Double).
 */
final case class DoubleArrayTile(array: Array[Double], cols: Int, rows: Int)
  extends MutableArrayTile with DoubleBasedArray {

  val cellType = TypeDouble

  def applyDouble(i: Int) = array(i)
  def updateDouble(i: Int, z: Double) = array(i) = z

  override def toArrayDouble = array.clone

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asDoubleBuffer.put(array)
    pixels
  }

  def warp(current: Extent, target: RasterExtent): ArrayTile = {
    val warped = Array.ofDim[Double](target.cols * target.rows).fill(Double.NaN)
    Warp[Double](RasterExtent(current, cols, rows), target, array, warped)
    DoubleArrayTile(warped, target.cols, target.rows)
  }
}

object DoubleArrayTile {
  def ofDim(cols: Int, rows: Int): DoubleArrayTile = 
    new DoubleArrayTile(Array.ofDim[Double](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): DoubleArrayTile = 
    new DoubleArrayTile(Array.ofDim[Double](cols * rows).fill(Double.NaN), cols, rows)

  def fill(v: Double, cols: Int, rows: Int): DoubleArrayTile =
    new DoubleArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)

  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int): DoubleArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val doubleBuffer = byteBuffer.asDoubleBuffer()
    val doubleArray = new Array[Double](bytes.length / TypeDouble.bytes)
    doubleBuffer.get(doubleArray)

    DoubleArrayTile(doubleArray, cols, rows)
  }
}
