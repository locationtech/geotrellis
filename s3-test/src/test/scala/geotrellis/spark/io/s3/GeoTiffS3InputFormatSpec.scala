package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.testkit._

import org.apache.hadoop.conf.Configuration
import com.amazonaws.auth.AWSCredentials
import org.apache.hadoop.mapreduce.{ TaskAttemptContext, InputSplit }
import org.scalatest._

import java.nio.file.{ Paths, Files }

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

      S3InputFormat.setCreateS3Client(job, { () => new MockS3Client })

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }

    it("should set the CRS") {
      val job = sc.newJob("geotiff-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setCreateS3Client(job, { () => new MockS3Client })
      GeoTiffS3InputFormat.setCrs(job, WebMercator)

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }
  }
}
