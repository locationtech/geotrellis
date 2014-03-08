package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles.AllOnes

import org.scalatest.FunSpec

class AddSpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Add Operation") {
    val allOnes = AllOnes(inputHome, conf)

    it("should add a constant to a raster") { 

      val ones = RasterRDD(allOnes.path, sc)
      
      val twos = ones + 1

      shouldBe(twos, (2, 2, allOnes.tileCount))
    }

    it("should add multiple rasters") {

      val ones = RasterRDD(allOnes.path, sc)

      val threes = ones + ones + ones 

      shouldBe(threes, (3, 3, allOnes.tileCount))
    }
  }
}
