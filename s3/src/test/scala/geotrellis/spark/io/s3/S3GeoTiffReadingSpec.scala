package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.spark.io.s3._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._

import java.nio.{ByteBuffer, ByteOrder}
import org.scalatest._
import com.amazonaws.services.s3.model._

class S3GeoTiffReadingSepc extends FunSpec
  with Matchers
  with RasterMatchers
  with TileBuilders {

  /*
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
  */

  describe("Reading GeoTiff from server") {
    val client = S3Client.default
    val bucket = "gt-rasters"
    val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
    val s3ByteReader = S3ByteReader(bucket, k, client, 5000)
    val fromLocal = GeoTiffReader.readSingleband("../nlcd_2011_01_01.tif", false, true)
    val fromServer = GeoTiffReader.readSingleband(s3ByteReader, false, true)
    fromServer.crop(fromLocal.extent)

    //println(fromLocal)
    //println(fromServer)

    /*
    println("\n\n") 
    val s3ByteReader2 = S3ByteReader(bucket, k, client, 25000)
    val fromServer2 = GeoTiffReader.readSingleband(s3ByteReader2, false, true)
    
    println(fromLocal)
    println("\n")
    println(fromServer2)
    */
  }
}
