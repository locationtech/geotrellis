package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._
import geotrellis.spark

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling._
import geotrellis.raster.op.local._
import geotrellis.spark.utils.SparkUtils
import geotrellis.proj4.LatLng

import org.apache.spark._
import org.apache.spark.rdd._
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.Matchers._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs.Path

class AccumuloCatalogSpec extends FunSpec
  with Matchers
  with TestEnvironment
  with OnlyIfCanRunSpark
{

  describe("Accumulo Catalog") {
    ifCanRunSpark { 

      val accumulo = new AccumuloInstance(
        instanceName = "fake",
        zookeeper = "localhost",
        user = "root",
        token = new PasswordToken("")
      )
      val catalog = accumulo.catalog

      val allOnes = new Path(inputHome, "all-ones.tif")
      val source = sc.hadoopGeoTiffRDD(allOnes)
      val tableOps = accumulo.connector.tableOperations()
      val layoutScheme = ZoomedLayoutScheme(512)
      tableOps.create("tiles")

      val (level, onesRdd) = Ingest(source, LatLng, layoutScheme)

      it("should succeed writing to a table"){
        catalog.save(LayerId("ones", level.zoom), "tiles", onesRdd)
      }

      it("should load out saved tiles"){
        catalog.load[SpatialKey](LayerId("ones", 10)).count should be > 0l
      }

      it("should load out a single tile"){
        val tile = catalog.loadTile(LayerId("ones", 10), SpatialKey(917, 616))
        (tile.cols, tile.rows) should be ((512, 512))
      }

      it("should load out saved tiles, but only for the right zoom"){
        intercept[LayerNotFoundError] {
          catalog.load[SpatialKey](LayerId("ones", 9)).count()
        }
      }

      it("fetch a TileExtent from catalog"){
        val tileBounds = GridBounds(915,305,916,306)
        val filters = new FilterSet[SpatialKey] withFilter SpaceFilter(tileBounds)
        val rdd1 = catalog.load[SpatialKey](LayerId("ones", 10), filters)
        val rdd2 = catalog.load[SpatialKey](LayerId("ones", 10), filters)

        val out = rdd1.combinePairs(rdd2){case (tms1, tms2) =>
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
