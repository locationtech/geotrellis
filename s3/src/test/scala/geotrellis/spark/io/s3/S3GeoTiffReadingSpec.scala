package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.spark.io.s3._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._

import java.nio.{ByteBuffer, ByteOrder}
import com.amazonaws.services.s3.model._
import org.scalatest._

class S3GeoTiffReadingSepc extends FunSpec
  with Matchers
  with RasterMatchers {

  describe("Reading from a local geotiff") {
    val fromLocal =
      GeoTiffReader.readSingleband(
        "raster-test/data/geotiff-test-files/multi-tag.tif", false, true)

    val extent = fromLocal.extent
    
    val testArray =
      Filesystem
        .slurp("raster-test/data/geotiff-test-files/multi-tag.tif")
    
    val chunkSize = 10
  
    val byteOrder: ByteOrder =
      (testArray(0).toChar, testArray(1).toChar) match {
        case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
        case ('M', 'M') => ByteOrder.BIG_ENDIAN
        case _ => throw new Exception("incorrect byte order")
      }

    val mock = new MockS3ByteReader(chunkSize, testArray, Some(byteOrder))
    val fromServer = GeoTiffReader.readSingleband(mock, false, true)

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
    val client = S3Client.default
    val bucket = "gt-rasters"
    val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
    val chunkSize = 500000
    val s3Bytes = S3StreamBytes(bucket, k, client, chunkSize)
    val s3ByteReader = S3ByteReader(s3Bytes)

    val fromLocal =
      GeoTiffReader.readSingleband("../nlcd_2011_01_01.tif", false, true)
    val fromServer =
      GeoTiffReader.readSingleband(s3ByteReader, false, true)
    
    val extent = fromLocal.extent

    it("should return the same geotiff") {
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
}
