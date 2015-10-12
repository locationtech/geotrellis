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

package geotrellis.spark.io.hadoop.formats

import geotrellis.raster._
import geotrellis.spark.io.hadoop._
import org.scalatest._
import scala.reflect._

class AvroWritableSpec extends FunSpec with Matchers {
  def initAvroWritable[T, TW <: AvroWritable[T]: ClassTag](t: T): TW = {
    val tw = classTag[TW].runtimeClass.newInstance().asInstanceOf[TW]
    tw.set(t)
    tw
  }

  describe("conversion from/to TileWritable") {

    val cols = 2
    val rows = 2
    val size = cols * rows
    
    it("should convert from Array of ints to TileWritable and back") {
      val expected = Array.fill[Int](size)(1)
      val actual = initAvroWritable[Tile, TileWritable](IntArrayTile(expected, cols, rows))
      expected should be(actual.get().asInstanceOf[IntArrayTile].array)
    }

    it("should convert from Array of shorts to TileWritable and back") {
      val expected = Array.fill[Short](size)(1)
      val actual = initAvroWritable[Tile, TileWritable](ShortArrayTile(expected, cols, rows)).get()
      expected should be(actual.asInstanceOf[ShortArrayTile].array)
    }

    it("should convert from Array of doubles to TileWritable and back") {
      val expected = Array.fill[Double](size)(1)
      val actual = initAvroWritable[Tile, TileWritable](DoubleArrayTile(expected, cols, rows)).get()
      expected should be(actual.asInstanceOf[DoubleArrayTile].array)
    }

    it("should convert from Array of floats to TileWritable and back") {
      val expected = Array.fill[Float](size)(1)
      val actual = initAvroWritable[Tile, TileWritable](FloatArrayTile(expected, cols, rows)).get()
      expected should be(actual.asInstanceOf[FloatArrayTile].array)
    }

    it("should convert from Array of bytes to TileWritable and back") {
      val expected = Array.fill[Byte](size)(1)
      val actual = initAvroWritable[Tile, TileWritable](ByteArrayTile(expected, cols, rows)).get()
      expected should be(actual.asInstanceOf[ByteArrayTile].array)
    }

    it("should convert from Array of bytes (actually, bit masks) to TileWritable and back") {
      val expected = Array.fill[Byte](size)(1)
      
      // bit mask length is 8x4 since there are 4 bytes of length 8 bits each
      val cols = 8
      val rows = 4
      
      val actual = initAvroWritable[Tile, TileWritable](BitArrayTile(expected, cols, rows)).get()
      expected should be(actual.asInstanceOf[BitArrayTile].array)
    }
  }
}
