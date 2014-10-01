package geotrellis.spark.ingest

import geotrellis.spark.ingest.AccumuloIngestCommand._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.rdd.{RasterRDD, TmsRasterRDD, LayerMetaData}
import org.apache.accumulo.core.client.Connector
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest._
import geotrellis.proj4.LatLng
import geotrellis.spark.tiling.TilingScheme
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.{TileId, TmsTile, OnlyIfCanRunSpark, TestEnvironment}
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import geotrellis.spark.io.hadoop._


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
      val catalog = accumulo.tileCatalog

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sparkContext.hadoopGeoTiffRDD(allOnes)
      val sink = { (tiles: RasterRDD[TileId]) =>
        catalog.save(tiles, "ones", "tiles")
      }

      {//we should not expect catalog to  create the table
        val tableOps = accumulo.connector.tableOperations()
        tableOps.create("tiles")
      }

      it("should provide a sink for Ingest") {
        Ingest(sparkContext)(source, sink, LatLng, TilingScheme.TMS)
      }

      it("should have saved only one layer with default sink") {
        catalog.load[TileId]("ones", 10) should  not be empty //base layer based on resolution
        catalog.load[TileId]("ones", 9) should be (empty)     //didn't pyramid
      }

      it("should work with pyramid sink"){
        Ingest(sparkContext)(source, Ingest.pyramid(sink), LatLng, TilingScheme.TMS)
        for (level <- 10 to 1 by -1) {
          val rdd = catalog.load[TileId]("ones", level)
          rdd should not be empty
          //println(s"Level: $level, tiles: ${rdd.get.count}")
        }
      }
    }
  }
}
