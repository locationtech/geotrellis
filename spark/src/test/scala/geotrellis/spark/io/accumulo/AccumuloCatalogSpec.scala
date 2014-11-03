package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._

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
import org.scalatest._
import org.scalatest.Matchers._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

import org.apache.hadoop.fs.Path
import scala.util.{Try, Success, Failure}

class AccumuloCatalogSpec extends FunSpec
  with Matchers
  with TestEnvironment
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
      val layoutScheme = ZoomedLayoutScheme()
      tableOps.create("tiles")

      val (onesMd, onesRdd) = Ingest(source, "ones", LatLng, layoutScheme)      
     
      it("should fail writing to no table"){        
        intercept[TableNotFoundError] {
          catalog.save(onesMd.id, onesRdd, "NOTiles")          
        }
      }

      it("should succeed writing to a table"){
        catalog.save(onesMd.id, onesRdd, "tiles")          
      }

      it("should load out saved tiles"){
        catalog.load[SpatialKey](LayerId("ones", 10)).get.count should be > 0l
      }

      it("should load out saved tiles, but only for the right zoom"){
        intercept[LayerNotFoundError] {
          catalog.load[SpatialKey](LayerId("ones", 9)).get.count()
        }
      }

      it("fetch a TileExtent from catalog"){
        val tileBounds = GridBounds(915,305,916,306)
        val filters = FilterSet.EMPTY[SpatialKey] withFilter SpaceFilter(tileBounds)
        val rdd1 = catalog.load[SpatialKey](LayerId("ones", 10), filters).get
        val rdd2 = catalog.load[SpatialKey](LayerId("ones", 10), filters).get

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
