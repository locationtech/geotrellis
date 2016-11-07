package geotrellis.spark.io.s3.util


import java.nio.file.{Files, Paths}
import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.testkit._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._

import java.nio.{ByteBuffer, ByteOrder}
import com.amazonaws.services.s3.model._
import org.scalatest._

class S3GeoTiffReadingSpec extends FunSpec
  with Matchers
  with RasterMatchers {

  val bucket = this.getClass.getSimpleName

  describe("Reading from a local geotiff") {
    val fromLocal =
      GeoTiffReader.readSingleband(
        "raster-test/data/geotiff-test-files/multi-tag.tif", false, true)

    val extent = fromLocal.extent

    val testArray =
      Filesystem
        .slurp("raster-test/data/geotiff-test-files/multi-tag.tif")

    val mockClient = new MockS3Client

    mockClient.putObject(bucket,
      "geotiff/multi-tag.tif",
      testArray)


    val reader = StreamingByteReader(S3RangeReader(bucket, "geotiff/multi-tag.tif", mockClient))
    val fromServer = GeoTiffReader.readSingleband(reader, false, true)

    it("should return the same geoTiff") {
      assertEqual(fromLocal, fromServer)
    }

    it("should return the same cropped geotiff, edge") {
      val e = Extent(extent.xmin, extent.ymin, extent.xmax - 2, extent.ymax - 3)
      val actual = fromServer.crop(e)
      val expected = fromLocal.crop(e)

      assertEqual(actual, expected)
    }

    it("should return the same cropped geotiff, center") {
      val e = Extent(extent.xmin + 1, extent.ymin + 2, extent.xmax - 2, extent.ymax - 3)
      val actual = fromServer.crop(e)
      val expected = fromLocal.crop(e)

      assertEqual(actual, expected)
    }
  }

  describe("Reading GeoTiff from server") {
    val mockClient = new MockS3Client
    val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))

    mockClient.putObject(bucket,
      "geotiff/all-ones.tif",
      geoTiffBytes)

    val chunkSize = 256000
    val request = new GetObjectRequest(this.getClass.getSimpleName, "geotiff/all-ones.tif")
    val local = ByteBuffer.wrap(geoTiffBytes)

    val s3ByteReader = StreamingByteReader(S3RangeReader(request, mockClient))

    val fromLocal =
      GeoTiffReader.readSingleband(local, false, true)

    val fromServer =
      GeoTiffReader.readSingleband(s3ByteReader, false, true)

    val extent = fromLocal.extent

    it("should return the same geotiff") {
      assertEqual(fromLocal, fromServer)
    }

    it("should return the same cropped geotiff, edge") {
      val e = Extent(extent.xmin, extent.ymin, extent.xmax - 0.1, extent.ymax - 0.2)
      val actual = fromServer.crop(e)
      val expected = fromLocal.crop(e)

      assertEqual(actual, expected)
    }

    it("should return the same cropped geotiff, center") {
      val e = Extent(extent.xmin + 0.05, extent.ymin + 0.05, extent.xmax - 0.1, extent.ymax - 0.2)
      val actual = fromServer.crop(e)
      val expected = fromLocal.crop(e)

      assertEqual(actual, expected)
    }
  }
}
