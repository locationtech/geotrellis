package geotrellis.spark.io.s3

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import org.scalatest._

class GeoTiffS3InputFormatSpec extends FunSpec with TestEnvironment with Matchers {
  
  describe("GeoTiff S3 InputFormat") {      
    val url = "s3n://geotrellis-test/nlcd-geotiff"      

    it("should read GeoTiffs from S3") {
      val job = sc.newJob("geotiff-ingest")        
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x=>x).cache      
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng)){ (rdd, level) =>
        val rddCount = rdd.count
        rddCount should not be (0)
        info(s"Tiled RDD count: ${rddCount}")        
      }        
    }
  }
}
