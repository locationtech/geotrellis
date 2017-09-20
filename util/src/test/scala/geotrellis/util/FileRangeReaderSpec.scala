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

package geotrellis.util

import java.nio.ByteBuffer
import java.nio.file.{Paths, Files}

import org.scalatest._

class FileRangeReaderSpec extends FunSpec
  with Matchers {

  describe("Streaming bytes locally") {
    val path = "raster/data/aspect.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(path))
    val buffer = ByteBuffer.wrap(geoTiffBytes)
    val chunkSize = 2000
    val local = FileRangeReader(path)

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    it("should return the correct bytes") {
      val actual = local.readRange(0.toLong, chunkSize)

      val result = testArrays(actual, geoTiffBytes)

      result.length should be (0)
    }

    it("should return the correct bytes from the middle of the file") {
      val actual = local.readRange(250, chunkSize)
      val arr = buffer.array
      val expected = Array.ofDim[Byte](chunkSize)

      System.arraycopy(arr, 250, expected, 0, chunkSize)

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should not read past the end of the file") {
      val start = local.totalLength - 100
      val actual = local.readRange(start, start.toInt + 300)
      val arr = Array.ofDim[Byte](100)
      buffer.position(start.toInt)

      val expected = {
        for(i <- 0 until 100) {
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
