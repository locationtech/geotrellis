package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.TestEnvironmentFixture
import geotrellis.spark.testfiles.AllTwos
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.rdd.RasterHadoopRDD

class MultiplySpec extends TestEnvironmentFixture with RasterRDDMatchers {

  describe("Multiply Operation") {
    val allTwos = AllTwos(inputHome, conf)

    it("should multiply a constant by a raster") { sc =>

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val fours = twos * 2

      shouldBe(fours, (4, 4, allTwos.tileCount))
    }

    it("should multiply multiple rasters") { sc =>
      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)
      val eights = twos * twos * twos

      shouldBe(eights, (8, 8, allTwos.tileCount))
    }
  }
}