package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._

import geotrellis.raster.op.local._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.proj4.LatLng

import org.apache.spark._
import org.apache.spark.rdd._
import org.scalatest._
import org.scalatest.Matchers._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs.Path
import scala.util.{Try, Success, Failure}

class AccumuloCatalogSpec extends FunSpec
  with Matchers
  with TestEnvironment
  with RasterVerifyMethods
  with OnlyIfCanRunSpark
{

  describe("Accumulo Catalog") {
    ifCanRunSpark {
      implicit val sparkContext = SparkUtils.createSparkContext("local", "Accumulo Ingest Test")

      val accumulo = new AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )
      val catalog = accumulo.catalog

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sparkContext.hadoopGeoTiffRDD(allOnes)
      val tableOps = accumulo.connector.tableOperations()
      tableOps.create("tiles")

      val sink: (RasterRDD[TileId] => Unit) = { tiles =>
        catalog.save[TileId](tiles, "ones", "tiles").get
      }

      it("should fail to save without driver"){
        intercept[DriverNotFound[TileId]] {
          Ingest(sparkContext)(source, sink, LatLng, TilingScheme.TMS)
        }
      }

      it("should fail to load without driver"){
        intercept[DriverNotFound[TileId]] {
          catalog.load[TileId]("ones", 10).get.count
        }
      }

      it("should fail writing to no table"){
        catalog.register(RasterAccumuloDriver)
        intercept[TableNotFound] {
          Ingest(sparkContext)(source,
            { tiles =>catalog.save[TileId](tiles, "ones", "NOtiles").get },
            LatLng, TilingScheme.TMS)
        }
      }

      it("should provide a sink for Ingest") {
        Ingest(sparkContext)(source, sink, LatLng, TilingScheme.TMS)
      }

      it("should load out saved tiles"){
        catalog.load[TileId]("ones", 10).get.count should be > 0l
      }

      it("should load out saved tiles, but only for the right zoom"){
        intercept[LayerNotFound] {
          catalog.load[TileId]("ones", 9).get.count()
        }
      }


      it("be able to map the id to grid") {
        //saved from first run, mostly detects regressions
        val expected = List((915,305), (916,305), (917,305), (915,306), (916,306),
          (917,306), (915,307), (916,307), (917,307), (915,308), (916,308), (917,308))

        val rdd = catalog.load[TileId]("ones", 10).get
        rdd
          .map{ case (id, tile) => rdd.metaData.transform.indexToGrid(id)}
          .collect should be (expected)
      }

      it("fetch a TileExtent from catalog"){
        val tileBounds = GridBounds(915,305,916,306)
        val filters = FilterSet[TileId]() withFilter SpaceFilter(tileBounds, GridCoordScheme)
        val rdd1 = catalog.load[TileId]("ones", 10, filters).get
        val rdd2 = catalog.load[TileId]("ones", 10, filters).get

        val out = rdd1.combineTiles(rdd2){case (tms1, tms2) =>
          require(tms1.id == tms2.id)
          val res = tms1.tile.localAdd(tms2.tile)
          (tms1.id, res)
        }

        val tile = out.first.tile
        tile.get(497,511) should be (2)
      }
    }
  }
}
