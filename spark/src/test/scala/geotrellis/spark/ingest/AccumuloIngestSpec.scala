package geotrellis.spark.ingest

import geotrellis.spark.io.accumulo._
import org.scalatest._
import geotrellis.proj4.LatLng
import geotrellis.spark.tiling.TilingScheme
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark._
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

      {//we should not expect catalog to  create the table
      val tableOps = accumulo.connector.tableOperations()
        tableOps.create("tiles")
      }

      val catalog = new AccumuloCatalog(sparkContext, accumulo, accumulo.metaDataCatalog)
      catalog.register(RasterAccumuloDriver)

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sparkContext.hadoopGeoTiffRDD(allOnes)
      val sink = { (tiles: RasterRDD[TileId]) =>
        catalog.save(tiles, "ones", "tiles")
      }

      {//we should not expect catalog to  create the table
        val tableOps = accumulo.connector.tableOperations()
        if (! tableOps.exists("tiles")) tableOps.create("tiles")
      }

      it("should provide a sink for Ingest") {
        Ingest(sparkContext)(source, sink, LatLng, TilingScheme.TMS)
        println(accumulo.metaDataCatalog.fetchAll)
      }

      it("should load some tiles") {
        val rdd = catalog.load[TileId]("ones", 10)
        val rdd2 = catalog.load[TileId]("ones", 10)

        println("COUNT", rdd.get.count)
      }

      it("can be shoved into a MultiCatlog") {
//        val mc = new MultiCatalog

//        mc.includeCatalog(AccumuloTarget("asdf", "asdf"))
      }

    }
  }
}
