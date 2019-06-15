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

package geotrellis.spark.store.s3.util

import java.nio.file.{ Paths, Files }
import java.nio.ByteBuffer
import geotrellis.store.s3.util._
import geotrellis.util._
import geotrellis.spark.store.s3._
import geotrellis.spark.store.s3.testkit._
import spire.syntax.cfor._

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model._
import org.apache.commons.io.IOUtils

import org.scalatest._

class S3RangeReaderSpec extends FunSpec with Matchers {
  val bucket = this.getClass.getSimpleName.toLowerCase
  val mockClient = MockS3Client()
  S3TestUtils.cleanBucket(mockClient, bucket)

  describe("S3RangeReader") {
    val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))

    val putReq = PutObjectRequest.builder()
      .bucket(bucket)
      .key("geotiff/all-ones.tif")
      .build()
    val putBody = RequestBody.fromBytes(geoTiffBytes)
    mockClient.putObject(putReq, putBody)

    val chunkSize = 20000
    val request =
      GetObjectRequest.builder()
        .bucket(bucket)
        .key("geotiff/all-ones.tif")
        .build()
    val rangeReader = S3RangeReader(request, mockClient)

    val local = ByteBuffer.wrap(geoTiffBytes)

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    it("should return the correct bytes") {
      val actual = rangeReader.readRange(0, chunkSize)
      val expected = Array.ofDim[Byte](chunkSize)

      cfor(0)(_ < chunkSize, _ + 1) { i=>
        expected(i) = local.get
      }
      local.position(0)

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should return the correct bytes throught the file") {
      cfor(0)(_ < rangeReader.totalLength - chunkSize, _ + chunkSize){ i =>
        val actual = rangeReader.readRange(i, chunkSize)
        val expected = Array.ofDim[Byte](chunkSize)

        cfor(0)(_ < chunkSize, _ + 1) { j =>
          expected(j) = local.get
        }

        val result = testArrays(actual, expected)

        result.length should be (0)
      }
      local.position(0)
    }

    it("should not read past the end of the file") {
      val start = rangeReader.totalLength - 100
      val actual = rangeReader.readRange(start, (start + 300).toInt)
      val arr = Array.ofDim[Byte](100)
      local.position(start.toInt)

      val expected = {
        cfor(0)(_ < 100, _ + 1){ i =>
          arr(i) = local.get
        }
        arr
      }
      local.position(0)

      val result = testArrays(expected, actual)

      result.length should be (0)
    }
  }
}
