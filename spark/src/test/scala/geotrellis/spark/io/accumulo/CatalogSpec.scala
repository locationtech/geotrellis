package geotrellis.spark.io.accumulo

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.spark.ingest._
import geotrellis.spark.rdd._
import org.apache.spark._
import org.apache.spark.rdd._
import org.scalatest._
import geotrellis.proj4.LatLng
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs.Path
import geotrellis.spark.io.hadoop._


class CatalogSpec extends FunSpec
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
      val catalog = accumulo.tileCatalog

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sparkContext.hadoopGeoTiffRDD(allOnes)
      val tableOps = accumulo.connector.tableOperations()
      tableOps.create("tiles")

      it("should provide a sink for Ingest") {
        val sink = { (tiles: RDD[TmsTile], metaData: LayerMetaData) =>
          val raster: TmsRasterRDD = new TmsRasterRDD(tiles, metaData)
          catalog.save(raster, "ones", "tiles")
        }
        Ingest(sparkContext)(source, sink, LatLng, TilingScheme.TMS)
      }

      it("be able to map the id to grid") {
        val rdd = catalog.load(Layer("ones", 10)).get
        rdd
          .map{ case TmsTile(id, tile) => (rdd.metaData.transform.indexToGrid(id))}
          .collect//.foreach(println)
      }

      it("fetch a TileExtent from catalog"){
        val tileBounds = GridBounds(915,305,916,306)
        val rdd1 = catalog.load(Layer("ones", 10), Some(tileBounds -> GridCoordScheme)).get
        val rdd2 = catalog.load(Layer("ones", 10), Some(tileBounds -> GridCoordScheme)).get

        val out = rdd1.combineTiles(rdd2){case (tms1, tms2) =>
          require(tms1.id == tms2.id)
          val res = tms1.tile.localAdd(tms2.tile)
          TmsTile(tms1.id, res)
        }

        val tile = out.first.tile
        tile.get(497,511) should be (2)
      }
    }
  }
}
