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
import org.scalatest._

class TemporalGeoTiffS3InputFormatSpec extends FunSpec with Matchers with OnlyIfCanRunSpark {
  val layoutScheme = ZoomedLayoutScheme(LatLng)

  describe("Temporal GeoTiff S3 InputFormat"){
    it("should read a custom tiff tag and format") {
      val path = "src/test/resources/test-time-tag.tif"

      val format = new TemporalGeoTiffS3InputFormat
      val conf = new Configuration(false)
      conf.set(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG, "TIFFTAG_DATETIME")
      conf.set(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT, "2015:03:25 18:01:04")
      val context = new TaskAttemptContextImpl(conf, new TaskAttemptID())
      val rr = format.createRecordReader(null, context)
      val (key, tile) = rr.read(Filesystem.slurp(path))
    }

    ifCanRunSpark {
      it("should read GeoTiffs with ISO_TIME tag from S3") {
        val url = "s3n://geotrellis-test/nex-geotiff/"
        val job = sc.newJob("temporal-geotiff-ingest")        
        S3InputFormat.setUrl(job, url)
        S3InputFormat.setAnonymous(job)

        val conf = job.getConfiguration
        conf.set(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG, "ISO_TIME")
        conf.set(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT, "yyyy-MM-dd'T'HH:mm:ss")

        val source = sc.newAPIHadoopRDD(job.getConfiguration,
          classOf[TemporalGeoTiffS3InputFormat],
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
}
