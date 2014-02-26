package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.TestEnvironmentFixture
import geotrellis.spark.testfiles.AllTwos
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.rdd.RasterHadoopRDD

class DivideSpec extends TestEnvironmentFixture with RasterRDDMatchers {

  describe("Divide Operation") {
    val allTwos = AllTwos(inputHome, conf)
    val allHundreds = AllHundreds(inputHome, conf)

    it("should divide raster values by a constant") { sc =>

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = twos / 2

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should divide from a constant, raster values") { sc =>

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = 2 /: twos

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should divide multiple rasters") { sc =>
      val hundreds = RasterHadoopRDD.toRasterRDD(allHundreds.path, sc)
      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)
      val res = hundreds / twos / twos

      shouldBe(res, (25, 25, allHundreds.tileCount))
    }
  }
}