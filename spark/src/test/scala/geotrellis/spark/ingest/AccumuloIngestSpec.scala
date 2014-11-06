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
  with OnlyIfCanRunSpark
{

  describe("Accumulo Ingest") {
    ifCanRunSpark { 
      val accumulo = new AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)

      val tableOps = accumulo.connector.tableOperations()
      tableOps.create("tiles")

      val (md, rdd) = Ingest[ProjectedExtent, SpatialKey](source, "ones", LatLng, ZoomedLayoutScheme())
      var tileCount: Long = rdd.count
      
      it("should save some tiles"){
        accumulo.catalog.save(LayerId("ones", 10), rdd, "tiles", true).get  
      }

      it("should load some tiles") {
        val out = accumulo.catalog.load[SpatialKey](LayerId("ones", 10))
        out.get.count should be (tileCount)
      }
    }
  }
}
