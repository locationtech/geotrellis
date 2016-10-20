package geotrellis.spark.io.s3

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.file.{ Path, Paths, Files }

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io.s3.util._
import geotrellis.spark.io.hadoop._

import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.task._
import org.apache.hadoop.conf.Configuration
import org.scalatest._

class MockGeoTiffS3InputFormat extends GeoTiffS3InputFormat {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new MockGeoTiffS3RecordReader
}

class MockGeoTiffS3RecordReader extends GeoTiffS3RecordReader {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
  def read(key: String, byteOrder: ByteOrder, s3Bytes: MockS3StreamBytes) = {
    val reader = new MockS3ByteReader(s3Bytes, Some(byteOrder))
    val geoTiff = SinglebandGeoTiff(reader)
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (ProjectedExtent(extent, crs), tile)
  }
}

class GeoTiffS3InputFormatSpec extends FunSpec with TestEnvironment with Matchers with RasterMatchers {

  val mockClient = new MockS3Client
  val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
  val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
  mockClient.putObject(this.getClass.getSimpleName, "geotiff/all-ones.tif", geoTiffBytes)

  describe("GeoTiff S3 InputFormat") {
    val url = s"s3n://${this.getClass.getSimpleName}/geotiff"

    it("should read GeoTiffs from S3") {
      val job = sc.newJob("geotiff-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[MockGeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")

      /**
        * Actually failes due to LatLon issue: https://github.com/geotrellis/geotrellis/issues/1341
        */
      /*Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng)){ (rdd, level) =>
        val rddCount = rdd.count
        rddCount should not be (0)
        info(s"Tiled RDD count: ${rddCount}")
      }*/
    }

    it("should read a file locally via S3Bytestreamer") {
      val chunkSize: Int = 20
      val byteOrder: ByteOrder =
        (geoTiffBytes(0).toChar, geoTiffBytes(1).toChar) match {
          case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
          case ('M', 'M') => ByteOrder.BIG_ENDIAN
          case _ => throw new Exception("incorrect byte order")
        }

      val s3Bytes = new MockS3ArrayBytes(chunkSize, geoTiffBytes)
      val format = new MockGeoTiffS3InputFormat
      val config = new Configuration(false)

      val context = new TaskAttemptContextImpl(conf, new TaskAttemptID())
      val rr = format.createRecordReader(null, context)
      val (key, actual) = rr.read("key", byteOrder, s3Bytes)

      val expected = SinglebandGeoTiff(geoTiffBytes)

      assertEqual(expected.tile, actual)
    }
    
    it("should read a file from S3 via S3Bytestreamer") {
      val chunkSize: Int = 20
      val length: Long = geoTiffBytes.length.toLong
      val request = new GetObjectRequest(this.getClass.getSimpleName, "geotiff/all-ones.tif")
      val byteOrder: ByteOrder =
        (geoTiffBytes(0).toChar, geoTiffBytes(1).toChar) match {
          case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
          case ('M', 'M') => ByteOrder.BIG_ENDIAN
          case _ => throw new Exception("incorrect byte order")
        }

      val s3Bytes = new MockS3Stream(chunkSize, length, request)
      val format = new MockGeoTiffS3InputFormat
      val config = new Configuration(false)

      val context = new TaskAttemptContextImpl(conf, new TaskAttemptID())
      val rr = format.createRecordReader(null, context)
      val (key, actual) = rr.read("key", byteOrder, s3Bytes)

      val expected = SinglebandGeoTiff(geoTiffBytes)

      assertEqual(expected.tile, actual)
    }
  }
}
