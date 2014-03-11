/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster

import geotrellis._
import java.nio.ByteBuffer

/**
 * RasterData based on Array[Float] (each cell as a Float).
 */
final case class FloatArrayRasterData(array: Array[Float], cols: Int, rows: Int)
  extends MutableRasterData with DoubleBasedArray {
  def getType = TypeFloat
  def alloc(cols: Int, rows: Int) = FloatArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i: Int) = array(i).toDouble
  def updateDouble(i: Int, z: Double) = array(i) = z.toFloat
  def copy = FloatArrayRasterData(array.clone, cols, rows)

  def toArrayByte: Array[Byte] = {
    val pixels = new Array[Byte](array.length * getType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asFloatBuffer.put(array)
    pixels
  }

  def warp(current:RasterExtent,target:RasterExtent):RasterData = {
    val warped = Array.ofDim[Float](target.cols*target.rows).fill(Float.NaN)
    Warp[Float](current,target,array,warped)
    FloatArrayRasterData(warped, target.cols, target.rows)
  }
}

object FloatArrayRasterData {
  def ofDim(cols: Int, rows: Int) = 
    new FloatArrayRasterData(Array.ofDim[Float](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = 
    new FloatArrayRasterData(Array.ofDim[Float](cols * rows).fill(Float.NaN), cols, rows)

  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int) = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val floatBuffer = byteBuffer.asFloatBuffer()
    val floatArray = new Array[Float](bytes.length / TypeFloat.bytes)
    floatBuffer.get(floatArray)

    FloatArrayRasterData(floatArray, cols, rows)
  }
}
