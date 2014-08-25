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

import spire.syntax.cfor._
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
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asDoubleBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile = 
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Double](target.cols * target.rows).fill(Double.NaN)
        Resample[Double](RasterExtent(current, cols, rows), target, array, resampled)
        DoubleArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}

object DoubleArrayTile {
  def ofDim(cols: Int, rows: Int): DoubleArrayTile = 
    new DoubleArrayTile(Array.ofDim[Double](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): DoubleArrayTile = 
    new DoubleArrayTile(Array.ofDim[Double](cols * rows).fill(Double.NaN), cols, rows)

  def fill(v: Double, cols: Int, rows: Int): DoubleArrayTile =
    new DoubleArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): DoubleArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val doubleBuffer = byteBuffer.asDoubleBuffer()
    val doubleArray = new Array[Double](bytes.size / TypeDouble.bytes)
    doubleBuffer.get(doubleArray)

    DoubleArrayTile(doubleArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Double): DoubleArrayTile = 
    if(isNoData(replaceNoData))
    fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
      val doubleBuffer = byteBuffer.asDoubleBuffer()
      val len = bytes.size / TypeDouble.bytes
      val doubleArray = new Array[Double](len)

      cfor(0)(_ < len, _ + 1) { i =>
        val v = doubleBuffer.get(i)
        if(v == replaceNoData) 
          doubleArray(i) = Double.NaN
        else
          doubleArray(i) = v
      }

      DoubleArrayTile(doubleArray, cols, rows)
    }
}
