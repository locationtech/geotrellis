package geotrellis.spark.io.s3

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.task._
import org.joda.time.format._
import org.scalatest._

class SpaceTimeGeoTiffS3InputFormatSpec extends FunSpec with Matchers with TestEnvironment {
  val layoutScheme = ZoomedLayoutScheme(LatLng)

  describe("SpaceTime GeoTiff S3 InputFormat") {
    it("should read the time from a file") {
      val path = new java.io.File(inputHomeLocalPath, "test-time-tag.tif").getPath

      val format = new SpaceTimeGeoTiffS3InputFormat
      val conf = new Configuration(false)

      SpaceTimeGeoTiffS3InputFormat.setTimeTag(conf, "TIFFTAG_DATETIME")
      SpaceTimeGeoTiffS3InputFormat.setTimeFormat(conf, "2015:03:25 18:01:04")

      val context = new TaskAttemptContextImpl(conf, new TaskAttemptID())
      val rr = format.createRecordReader(null, context)
      val (key, tile) = rr.read("key", Filesystem.slurp(path))
      DateTimeFormat.forPattern("2015:03:25 18:01:04").print(key.time) should be ("2015:03:25 18:01:04")
    }

    it("should read GeoTiffs with ISO_TIME tag from S3") {
      val url = "s3n://geotrellis-test/nex-geotiff/"
      val job = sc.newJob("temporal-geotiff-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)
      SpaceTimeGeoTiffS3InputFormat.setTimeTag(job, "ISO_TIME")
      SpaceTimeGeoTiffS3InputFormat.setTimeFormat(job, "yyyy-MM-dd'T'HH:mm:ss")

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[SpaceTimeGeoTiffS3InputFormat],
        classOf[SpaceTimeInputKey],
        classOf[Tile])

      source.cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")

      Ingest[SpaceTimeInputKey, SpaceTimeKey](source, LatLng, layoutScheme){ (rdd, level) =>
        val rddCount = rdd.count
        rddCount should not be (0)
        info(s"Tiled RDD count: ${rddCount}")
      }
    }
  }
}
