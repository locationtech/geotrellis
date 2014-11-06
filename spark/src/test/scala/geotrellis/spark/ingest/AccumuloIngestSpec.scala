package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.proj4.LatLng
import geotrellis.spark.utils.SparkUtils

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.scalatest._


class AccumuloIngestSpec extends FunSpec
  with Matchers
  with TestEnvironment
  with RasterVerifyMethods
  with OnlyIfCanRunSpark
{

  describe("Accumulo Ingest") {
    ifCanRunSpark {
      implicit val sparkContext = SparkUtils.createSparkContext("local", "Accumulo Ingest Test")

      val accumulo = new AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sparkContext.hadoopGeoTiffRDD(allOnes)

      val ingest = new AccumuloIngest[ProjectedExtent, SpatialKey](accumulo.catalog, ZoomedLayoutScheme())

      ingest(source, "ones", LatLng)

      it("should load some tiles") {
        val rdd = accumulo.catalog.load[SpatialKey](LayerId("ones", 10))

        println("COUNT", rdd.get.count)
      }
    }
  }
}
