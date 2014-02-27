package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.testfiles.AllTwos

import org.scalatest.FunSpec

class SubtractSpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Subtract Operation") {
    val allTwos = AllTwos(inputHome, conf)
    val allHundreds = AllHundreds(inputHome, conf)

    it("should subtract a constant from a raster") { 

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = twos - 1

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract from a constant, raster values") { 

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = 3 -: twos

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should subtract multiple rasters") { 
      val hundreds = RasterHadoopRDD.toRasterRDD(allHundreds.path, sc)
      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)
      val res = hundreds - twos - twos

      shouldBe(res, (96, 96, allHundreds.tileCount))
    }
  }
}