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

import geotrellis.raster.resample._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
final case class ShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeShort

  def apply(i: Int) = s2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2s(z) }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: ResampleMethod): ArrayTile = 
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Short](target.cols * target.rows).fill(shortNODATA)
        Resample[Short](RasterExtent(current, cols, rows), target, array, resampled)
        ShortArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}

object ShortArrayTile {
  def ofDim(cols: Int, rows: Int): ShortArrayTile = 
    new ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ShortArrayTile = 
    new ShortArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)

  def fill(v: Short, cols: Int, rows: Int): ShortArrayTile =
    new ShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / TypeShort.bytes)
    shortBuffer.get(shortArray)

    ShortArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): ShortArrayTile = 
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
      val shortBuffer = byteBuffer.asShortBuffer()
      val len = bytes.length / TypeShort.bytes
      val shortArray = new Array[Short](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val v = shortBuffer.get(i)
        if(v == replaceNoData)
          shortArray(i) = shortNODATA
        else
          shortArray(i) = v
      }

      ShortArrayTile(shortArray, cols, rows)
    }
}
