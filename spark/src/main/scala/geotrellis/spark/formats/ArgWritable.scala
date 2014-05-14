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

import geotrellis._
import geotrellis.RasterType
import geotrellis.raster.RasterData
import org.apache.hadoop.io.BytesWritable
import org.apache.spark.Logging

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) with Logging {
  /* This constructor is used by hadoop, (e.g., hadoop fs -text) and  
   * Spark (HadoopRDD.compute does reader.createValue()). Geotrellis 
   * developers are encouraged to use one of the "apply" methods instead
   */
  def this() = this(Array[Byte]())

  def rasterBytes(awType: RasterType, cols: Int, rows: Int): Int = awType match {
    case TypeBit => rows
    case _       => cols * rows * awType.bytes
  }
  def toRasterData(awType: RasterType, cols: Int, rows: Int) = {
    /* 
     * The slice is done in cases where the backing byte array in BytesWritable 
     * is larger than expected, so a simple getBytes would get the larger array
     * and RasterData would be thrown off. See BytesWritable.setSize, which calls
     * setCapacity with 1.5 * size if the array needs to be grown
     * 
     * BitArrayRasterData is a bit special since every row is a byte, cols = 8  
     *
     */
    RasterData.fromArrayByte(getBytes.slice(0, rasterBytes(awType, cols, rows)), awType, cols, rows)
  }
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte) = new ArgWritable(Array.ofDim[Byte](len).fill(fillValue))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def fromRasterData(data: RasterData) = ArgWritable(data.toArrayByte)
  def fromRaster(raster: Raster) = ArgWritable.fromRasterData(raster.data)
}
