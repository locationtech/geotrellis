/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.formats

import geotrellis.raster._

import org.apache.hadoop.io.BytesWritable
import org.apache.spark.Logging

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) with Logging {
  /* This constructor is used by hadoop, (e.g., hadoop fs -text) and  
   * Spark (HadoopRDD.compute does reader.createValue()). Geotrellis 
   * developers are encouraged to use one of the "apply" methods instead
   */
  def this() = this(Array[Byte]())

  def toTile(cellType: CellType, cols: Int, rows: Int): MutableArrayTile = {
    /* 
     * The slice is done in cases where the backing byte array in BytesWritable 
     * is larger than expected, so a simple getBytes would get the larger array
     * and Tile would be thrown off. See BytesWritable.setSize, which calls
     * setCapacity with 1.5 * size if the array needs to be grown
     * 
     * BitArrayTile is a bit special since every row is a byte, cols = 8  
     *
     */
    println(s"               INNNTAKE    $cols $rows ${getBytes.size} ${cellType.numBytes(cols*rows)}")
    val x = getBytes.slice(0, cellType.numBytes(cols * rows)).filter(_ != Byte.MinValue)
    if(!x.isEmpty)
      println(s"           INTAKE           ${x.max}")

    val t = ArrayTile.fromBytes(getBytes.slice(0, cellType.numBytes(cols * rows)), cellType, cols, rows)
    if(!x.isEmpty) {
      val x2 = t.toBytes.filter(_ != Byte.MinValue)
      if(!x2.isEmpty) {
        println(s"           INTAKE  2         ${x2.max}")
      } else {
        println(s"           INTAKE  2         EMPTY!!!")
      }
    }
    t

  }
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte): ArgWritable = 
    new ArgWritable(Array.ofDim[Byte](len).fill(fillValue))

  def apply(bytes: Array[Byte]): ArgWritable = 
    new ArgWritable(bytes)

  def apply(aw: ArgWritable): ArgWritable = 
    new ArgWritable(aw.getBytes)

  def fromTile(tile: Tile) = {
    val x = tile.toBytes
    println(s"           asdf           ${x.size}")
    ArgWritable(tile.toBytes)
  }
}
