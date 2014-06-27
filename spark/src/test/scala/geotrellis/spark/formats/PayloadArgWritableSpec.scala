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
import geotrellis.raster.TypeBit
import geotrellis.raster.TypeByte
import geotrellis.raster.TypeDouble
import geotrellis.raster.TypeFloat
import geotrellis.raster.TypeInt
import geotrellis.raster.TypeShort
import geotrellis.raster.BitArrayTile
import geotrellis.raster.ByteArrayTile
import geotrellis.raster.DoubleArrayTile
import geotrellis.raster.FloatArrayTile
import geotrellis.raster.IntArrayTile
import geotrellis.raster.ShortArrayTile

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class PayloadArgWritableSpec extends FunSpec with ShouldMatchers {
  describe("conversion from/to PayloadArgWritable") {

    val cols = 2
    val rows = 2
    val size = cols * rows
    val payload = TileIdWritable(100L)
    it("should convert from Array of ints and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Int](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable(IntArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeInt, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[IntArrayTile].array)
      payload should be(actualTW)
    }

    it("should convert from Array of shorts and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Short](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable(ShortArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeShort, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[ShortArrayTile].array)
      payload should be(actualTW)
    }

    it("should convert from Array of doubles and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Double](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable(DoubleArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeDouble, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[DoubleArrayTile].array)
      payload should be(actualTW)
    }

    it("should convert from Array of floats and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Float](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable(FloatArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeFloat, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[FloatArrayTile].array)
      payload should be(actualTW)
    }

    it("should convert from Array of bytes and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Byte](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable(ByteArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeByte, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[ByteArrayTile].array)
      payload should be(actualTW)
    }

    it("should convert from Array of bytes (actually, bit masks) to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Byte](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in

      // bit mask length is 8x4 since there are 4 bytes of length 8 bits each
      val cols = 8
      val rows = 4

      val actualRD =
        PayloadArgWritable(BitArrayTile(expectedRD, cols, rows), payload)
          .toPayloadTile(TypeBit, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[BitArrayTile].array)
      payload should be(actualTW)

    }

  }
}