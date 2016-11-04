package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.testkit._

import com.amazonaws.auth.AWSCredentials
import org.apache.hadoop.mapreduce.{ TaskAttemptContext, InputSplit }
import org.scalatest._

import java.nio.file.{ Paths, Files }

/*
class MockTiffTagsS3InputFormat extends TiffTagsS3InputFormat {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new MockTiffTagsS3RecordReader(context)
}

class MockTiffTagsS3RecordReader(context: TaskAttemptContext) extends TiffTagsS3RecordReader(context) {
  override def getS3Client(credentials: AWSCredentials): S3Client = new MockS3Client
}

class TiffTagsS3InputFormatSpec extends FunSpec with TestEnvironment with Matchers {
  val mockClient = new MockS3Client
  val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
  val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
  mockClient.putObject(this.getClass.getSimpleName, "geotiff/all-ones.tif", geoTiffBytes)

  describe("GeoTiff S3 InputFormat") {
    val url = s"s3n://${this.getClass.getSimpleName}/geotiff"

    it("should read GeoTiffs from S3") {
      val job = sc.newJob("tifftags-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)
      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[MockTiffTagsS3InputFormat],
        classOf[String],
        classOf[TiffTags])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }
  }
}
*/
