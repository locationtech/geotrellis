package geotrellis.spark.io.s3.util

import java.nio.file.{ Paths, Files }
import java.nio.ByteBuffer
import geotrellis.util._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.testkit._
import spire.syntax.cfor._

import com.amazonaws.services.s3.model._
import org.apache.commons.io.IOUtils

import org.scalatest._

class S3StreamBytesSpec extends FunSpec with Matchers {

  describe("Streaming bytes from S3") {
    val mockClient = new MockS3Client
    val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))

    mockClient.putObject(this.getClass.getSimpleName,
      "geotiff/all-ones.tif",
      geoTiffBytes)

    val chunkSize = 20000
    val request = new GetObjectRequest(this.getClass.getSimpleName, "geotiff/all-ones.tif")
    val s3Bytes = new MockS3Stream(chunkSize, geoTiffBytes.length.toLong, request)

    val local = ByteBuffer.wrap(geoTiffBytes)

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    it("should return the correct bytes") {
      val actual = s3Bytes.getArray(0.toLong)
      val expected = Array.ofDim[Byte](chunkSize)

      cfor(0)(_ < chunkSize, _ + 1) { i=>
        expected(i) = local.get
      }
      local.position(0)

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should return the correct bytes throught the file") {
      cfor(0)(_ < s3Bytes.objectLength - chunkSize, _ + chunkSize){ i =>
        val actual = s3Bytes.getArray(i.toLong, chunkSize.toLong)
        val expected = Array.ofDim[Byte](chunkSize)
        
        cfor(0)(_ < chunkSize, _ + 1) { j =>
          expected(j) = local.get
        }

        val result = testArrays(actual, expected)

        result.length should be (0)
      }
      local.position(0)
    }

    it("should return the correct offsets for each chunk") {
      val actual = Array.range(0, 420000, chunkSize).map(_.toLong)
      val expected = Array.ofDim[Long](400000 / chunkSize)
      var counter = 0

      cfor(0)(_ < 400000, _ + chunkSize){ i =>
        expected(counter) = s3Bytes.getMappedArray(i.toLong, chunkSize).head._1
        counter += 1
      }

      val result = testArrays(actual, expected)

      result.length should be (0)
    }

    it("should not read past the end of the file") {
      val start = s3Bytes.objectLength - 100
      val actual = s3Bytes.getArray(start, start + 300)
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
