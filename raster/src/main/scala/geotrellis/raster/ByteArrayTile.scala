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
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
final case class ByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeByte

  def apply(i: Int) = b2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2b(z) }

  def toBytes: Array[Byte] = array.clone

  def copy = ArrayTile(array.clone, cols, rows)
}

object ByteArrayTile {
  def ofDim(cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows).fill(byteNODATA), cols, rows)

  def fill(v: Byte, cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    ByteArrayTile(bytes.clone, cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): ByteArrayTile =
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val arr = bytes.clone
      cfor(0)(_ < arr.size, _ + 1) { i =>
        val v = bytes(i)
        if(v == replaceNoData)
          arr(i) = byteNODATA
        arr(i) = bytes(i)
      }
      ByteArrayTile(arr, cols, rows)
    }
}
