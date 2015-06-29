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
 * ArrayTile based on Array[Float] (each cell as a Float).
 */
final case class FloatArrayTile(array: Array[Float], cols: Int, rows: Int)
  extends MutableArrayTile with DoubleBasedArray {

  val cellType = TypeFloat

  def applyDouble(i: Int) = array(i).toDouble
  def updateDouble(i: Int, z: Double) = array(i) = z.toFloat

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asFloatBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

object FloatArrayTile {
  def ofDim(cols: Int, rows: Int): FloatArrayTile = 
    new FloatArrayTile(Array.ofDim[Float](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): FloatArrayTile = 
    new FloatArrayTile(Array.ofDim[Float](cols * rows).fill(Float.NaN), cols, rows)

  def fill(v: Float, cols: Int, rows: Int): FloatArrayTile =
    new FloatArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): FloatArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val floatBuffer = byteBuffer.asFloatBuffer()
    val floatArray = new Array[Float](bytes.size / TypeFloat.bytes)
    floatBuffer.get(floatArray)

    FloatArrayTile(floatArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Float): FloatArrayTile = 
    if(isNoData(replaceNoData)) {
      fromBytes(bytes, cols, rows)
    } else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
      val floatBuffer = byteBuffer.asFloatBuffer()
      val len = bytes.size / TypeFloat.bytes
      val floatArray = new Array[Float](len)

      cfor(0)(_ < len, _ + 1) { i =>
        val v = floatBuffer.get(i)
        if(v == replaceNoData) {
          floatArray(i) = Float.NaN
        }
        else
          floatArray(i) = v
      }

      FloatArrayTile(floatArray, cols, rows)
    }
}
