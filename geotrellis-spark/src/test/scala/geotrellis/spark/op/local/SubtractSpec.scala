package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.TestEnvironmentFixture
import geotrellis.spark.testfiles.AllTwos
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.rdd.RasterHadoopRDD

class SubtractSpec extends TestEnvironmentFixture with RasterRDDMatchers {

  describe("Subtract Operation") {
    val allTwos = AllTwos(inputHome, conf)
    val allHundreds = AllHundreds(inputHome, conf)

    it("should subtract a constant from a raster") { sc =>

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = twos - 1

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract from a constant, raster values") { sc =>

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = 3 -: twos

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract multiple rasters") { sc =>
      val hundreds = RasterHadoopRDD.toRasterRDD(allHundreds.path, sc)
      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)
      val res = hundreds - twos - twos

      shouldBe(res, (96, 96, allHundreds.tileCount))
    }
  }
}