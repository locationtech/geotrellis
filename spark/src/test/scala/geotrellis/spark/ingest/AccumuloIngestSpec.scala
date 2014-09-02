package geotrellis.spark.ingest

import org.scalatest._
import geotrellis.proj4.LatLng
import geotrellis.spark.tiling.TilingScheme
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.{OnlyIfCanRunSpark, TestEnvironment}
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import geotrellis.spark.io.hadoop._


class AccumuloIngestSpec extends FunSpec
  with Matchers
  with TestEnvironment
  with RasterVerifyMethods
  with OnlyIfCanRunSpark {

  describe("Accumulo Ingest") {
    ifCanRunSpark {
      val sparkContext = SparkUtils.createSparkContext("local", "Accumulo Ingest Test")
      val instance = new MockInstance()
      val connector = instance.getConnector("root", new PasswordToken(""))

      it("should provide a sink for Ingest") {
        try {
          val allOnes = new Path(inputHome, "all-ones.tif")
          val source = sparkContext.hadoopGeoTiffRDD(allOnes)
          val sink = AccumuloIngestCommand.accumuloSink("tiles", "test", connector)

          val tableOps = connector.tableOperations()
          tableOps.create("tiles")
          Ingest(sparkContext)(source, sink, LatLng, LatLng, TilingScheme.TMS)

        } finally {
          sparkContext.stop()
        }
      }
    }
  }
}
