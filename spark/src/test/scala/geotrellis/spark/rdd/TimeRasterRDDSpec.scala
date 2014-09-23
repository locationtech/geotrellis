package geotrellis.spark.rdd

import geotrellis.spark.TestEnvironment
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.io.hadoop._

import org.apache.hadoop.fs.Path

import org.scalatest._

class TimeRasterRDDSpec extends FunSpec with Matchers
  with TestEnvironment
{
  implicit val sparkContext = SparkUtils.createSparkContext("local", "GDAL Ingest Test")
  val allOnes = new Path("/Users/eugene/tmp/access1-0/historical", "BCSD_0.5deg_tasmin_Amon_access1-0_historical_r1i1p1_195001-200512.nc")
  val source = sparkContext.hadoopGdalRDD(allOnes)

  describe("NetCDF Import") {
    it("imports stuff") {
      source.map(_._1.bandMeta).foreach(println)
      // -> ((Extent, CRS, Time), Tile)
      // -> ((TileId, TimeId), Tile), LayerMetaData
    }
  }

}
