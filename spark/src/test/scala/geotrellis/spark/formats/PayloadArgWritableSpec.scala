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
import geotrellis.raster._

import org.scalatest._

class PayloadArgWritableSpec extends FunSpec with Matchers {
  describe("conversion from/to PayloadArgWritable") {

    val cols = 2
    val rows = 2
    val size = cols * rows
    val payload = TileIdWritable(100L)
    it("should convert from Array of ints and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Int](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(IntArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeInt, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[IntArrayRasterData].array)
      payload should be(actualTW)
    }

    it("should convert from Array of shorts and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Short](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(ShortArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeShort, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[ShortArrayRasterData].array)
      payload should be(actualTW)
    }

    it("should convert from Array of doubles and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Double](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(DoubleArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeDouble, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[DoubleArrayRasterData].array)
      payload should be(actualTW)
    }

    it("should convert from Array of floats and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Float](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(FloatArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeFloat, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[FloatArrayRasterData].array)
      payload should be(actualTW)
    }

    it("should convert from Array of bytes and a payload to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Byte](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in
      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(ByteArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeByte, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[ByteArrayRasterData].array)
      payload should be(actualTW)
    }

    it("should convert from Array of bytes (actually, bit masks) to PayloadArgWritable and back") {
      val expectedRD = Array.fill[Byte](size)(1)
      val actualTW = TileIdWritable(1L) // 1L is dummy - will be filled in

      // bit mask length is 8x4 since there are 4 bytes of length 8 bits each
      val cols = 8
      val rows = 4

      val actualRD =
        PayloadArgWritable.fromPayloadRasterData(BitArrayRasterData(expectedRD, cols, rows), payload)
          .toPayloadRasterData(TypeBit, cols, rows, actualTW)
      expectedRD should be(actualRD.asInstanceOf[BitArrayRasterData].array)
      payload should be(actualTW)

    }

  }
}