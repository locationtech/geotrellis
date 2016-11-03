/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import geotrellis.util.LocalBytesStreamer
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff._

import java.nio.ByteBuffer
import java.nio.file.{Paths, Files}

import spire.syntax.cfor._
import org.scalatest._

class LocalBytesStreamerSpec extends FunSpec
  with Matchers
  with GeoTiffTestUtils {

  describe("Streaming bytes locally") {
    val path = geoTiffPath("ls8_int32.tif")
    val geoTiffBytes = Files.readAllBytes(Paths.get(path))
    val buffer = ByteBuffer.wrap(geoTiffBytes)
    val chunkSize = 2000
    val local = LocalBytesStreamer(path, chunkSize)

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    it("should return the correct bytes") {
      val actual = local.getArray(0.toLong)
      
      val result = testArrays(actual, geoTiffBytes)

      result.length should be (0)
    }

    it("should return the correct bytes from the middle of the file") {
      val actual = local.getArray(250, chunkSize)
      val arr = buffer.array
      val expected = Array.ofDim[Byte](chunkSize)

      System.arraycopy(arr, 250, expected, 0, chunkSize)

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should return the correct offsets for each chunk") {
      val actual = Array.range(0, 42000, chunkSize).map(_.toLong)
      val expected = Array.ofDim[Long](40000 / chunkSize)
      var counter = 0

      cfor(0)(_ < 40000, _ + chunkSize){ i =>
        expected(counter) = local.getMappedArray(i.toLong, chunkSize).head._1
        counter += 1
      }

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should not read past the end of the file") {
      val start = local.objectLength - 100
      val actual = local.getArray(start, start + 300)
      val arr = Array.ofDim[Byte](100)
      buffer.position(start.toInt)

      val expected = {
        cfor(0)(_ < 100, _ + 1){ i =>
          arr(i) = buffer.get
        }
        arr
      }
      buffer.position(0)

      val result = testArrays(expected, actual)
      
      result.length should be (0)
    }
  }
}
