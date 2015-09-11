package geotrellis.spark.io.s3

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import org.scalatest._

class TemporalGeoTiffS3InputFormatSpec extends FunSpec with Matchers with OnlyIfCanRunSpark {
  val layoutScheme = ZoomedLayoutScheme(LatLng)

  describe("Temporal GeoTiff S3 InputFormat"){
    ifCanRunSpark {
      it("should read GeoTiffs with ISO_TIME tag from S3") {
        val url = "s3n://geotrellis-test/nex-geotiff/"
        val job = sc.newJob("temporal-geotiff-ingest")        
        S3InputFormat.setUrl(job, url)
        S3InputFormat.setAnonymous(job)

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
