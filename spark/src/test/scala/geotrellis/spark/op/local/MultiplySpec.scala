package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles.AllTwos

import org.scalatest.FunSpec

class MultiplySpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Multiply Operation") {
    val allTwos = AllTwos(inputHome, conf)

    it("should multiply a constant by a raster") { 

      val twos = RasterRDD(allTwos.path, sc)

      val fours = twos * 2

      shouldBe(fours, (4, 4, allTwos.tileCount))
    }

    it("should multiply multiple rasters") { 
      val twos = RasterRDD(allTwos.path, sc)
      val eights = twos * twos * twos

      shouldBe(eights, (8, 8, allTwos.tileCount))
    }
  }
}
