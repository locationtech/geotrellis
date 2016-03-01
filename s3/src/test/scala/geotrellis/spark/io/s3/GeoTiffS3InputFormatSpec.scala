package geotrellis.spark.io.s3

import java.nio.file.{ Paths, Files }

import com.amazonaws.auth.AWSCredentials
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import org.apache.hadoop.mapreduce.{ TaskAttemptContext, InputSplit }
import org.scalatest._

class MockGeoTiffS3InputFormat extends GeoTiffS3InputFormat {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new MockGeoTiffS3RecordReader
}

class MockGeoTiffS3RecordReader extends GeoTiffS3RecordReader {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
}

class GeoTiffS3InputFormatSpec extends FunSpec with TestEnvironment with Matchers {

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
  }
}
