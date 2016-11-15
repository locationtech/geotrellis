package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.GetObjectRequest
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.testkit._
import org.scalatest._
import java.nio.file.{ Paths, Files }


class TiffTagsS3InputFormatSpec extends FunSpec with Matchers with TestEnvironment {
  implicit val mockClient = new MockS3Client()
  val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
  val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
  mockClient.putObject(this.getClass.getSimpleName, "geotiff/all-ones.tif", geoTiffBytes)

  describe("TiffTagsS3InputFormat") {
    val url = s"s3n://${this.getClass.getSimpleName}/geotiff"

    it("should read GeoTiffs from S3") {
      val job = sc.newJob("tifftags-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)
      S3InputFormat.setCreateS3Client(job, () => new MockS3Client)
      val source = sc.newAPIHadoopRDD(
        job.getConfiguration,
        classOf[TiffTagsS3InputFormat],
        classOf[GetObjectRequest],
        classOf[TiffTags])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }
  }
}
