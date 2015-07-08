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
final case class IntArrayTile(array: Array[Int], cols: Int, rows: Int) 
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeInt

  def apply(i: Int) = array(i)
  def update(i: Int, z: Int) { array(i) = z }
  
  override def toArray = array.clone

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asIntBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

object IntArrayTile {
  def ofDim(cols: Int, rows: Int): IntArrayTile = 
    new IntArrayTile(Array.ofDim[Int](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): IntArrayTile = 
    new IntArrayTile(Array.ofDim[Int](cols * rows).fill(NODATA), cols, rows)

  def fill(v: Int, cols: Int, rows: Int): IntArrayTile =
    new IntArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
 
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): IntArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](bytes.size / TypeInt.bytes)
    intBuffer.get(intArray)

    IntArrayTile(intArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Int): IntArrayTile = 
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
      val intBuffer = byteBuffer.asIntBuffer()
      val len = bytes.size / TypeInt.bytes
      val intArray = new Array[Int](len)

      cfor(0)(_ < len, _ + 1) { i =>
        val v = intBuffer.get(i)
        if(v == replaceNoData)
          intArray(i) = NODATA
        else
          intArray(i) = v
      }

      IntArrayTile(intArray, cols, rows)
    }
}
