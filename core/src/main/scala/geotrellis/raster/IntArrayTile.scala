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
 * ArrayTile based on Array[Int] (each cell as an Int).
 */
final case class IntArrayTile(array: Array[Int], cols: Int, rows: Int) 
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeInt

  def apply(i: Int) = array(i)
  def update(i: Int, z: Int) { array(i) = z }
  
  override def toArray = array.clone

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asIntBuffer.put(array)
    pixels
  }

  def warp(current: Extent, target: RasterExtent): ArrayTile = {
    val warped = Array.ofDim[Int](target.cols * target.rows).fill(NODATA)
    Warp[Int](RasterExtent(current, cols, rows), target, array, warped)
    IntArrayTile(warped, target.cols, target.rows)
  }
}

object IntArrayTile {
  def ofDim(cols: Int, rows: Int): IntArrayTile = 
    new IntArrayTile(Array.ofDim[Int](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): IntArrayTile = 
    new IntArrayTile(Array.ofDim[Int](cols * rows).fill(NODATA), cols, rows)

  def fill(v: Int, cols: Int, rows: Int): IntArrayTile =
    new IntArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
 
  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int) = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](bytes.length / TypeInt.bytes)
    intBuffer.get(intArray)

    IntArrayTile(intArray, cols, rows)
  }
}
