package geotrellis.spark.rdd

import geotrellis.proj4.{LatLng, CRS}
import geotrellis.raster.{Tile, CellSize}
import geotrellis.spark.ingest.IngestNetCDF
import geotrellis.spark.ingest.IngestNetCDF.TimeBandTile
import geotrellis.spark.{TileId, TestEnvironment}
import geotrellis.spark.tiling._
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.accumulo._
import geotrellis.raster.reproject._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.data.{Value, Mutation}

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD

import org.scalatest._

class TimeRasterRDDSpec extends FunSpec with Matchers
  with TestEnvironment
{
  implicit val sparkContext = SparkUtils.createSparkContext("local", "GDAL Ingest Test")
  val rcp45_tasmin = new Path("/Users/eugene/tmp/access1-0/historical", "BCSD_0.5deg_tasmin_Amon_access1-0_historical_r1i1p1_195001-200512.nc")

  val accumulo = new AccumuloInstance(
    instanceName = "fake",
    zookeeper = "localhost",
    user = "root",
    token = new PasswordToken("")
  )
  val tableOps = accumulo.connector.tableOperations()
  tableOps.create("net")

  describe("NetCDF Import") {
    it("imports stuff") {
      //Read in multi-band NetCDF file(s)
      val source = sparkContext.netCdfRDD(rcp45_tasmin)

      var md: LayerMetaData = null

      val sink: IngestNetCDF.Sink = {rdd =>

        //normally we would fish out the metadata from the catalog
        md = rdd.metaData
        accumulo.saveRaster(rdd, "net", "rcp45")
      }

      IngestNetCDF(sparkContext)(source, sink, LatLng, TilingScheme.TMS)

      val out: RasterRDD[TimeBandTile] = accumulo.loadRaster(md, "net", "rcp45")
      println("RECORDS", out.count)
    }
  }

}
