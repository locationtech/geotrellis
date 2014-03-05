package geotrellis.spark.op.local

import geotrellis.spark.RasterRDDMatchers
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterHadoopRDD
import geotrellis.spark.testfiles.AllHundreds
import geotrellis.spark.testfiles.AllTwos

import org.scalatest.FunSpec

class DivideSpec extends FunSpec with TestEnvironment with SharedSparkContext with RasterRDDMatchers {

  describe("Divide Operation") {
    val allTwos = AllTwos(inputHome, conf)
    val allHundreds = AllHundreds(inputHome, conf)

    it("should divide raster values by a constant") { 

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = twos / 2

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should divide from a constant, raster values") { 

      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)

      val ones = 2 /: twos

      shouldBe(ones, (1, 1, allTwos.tileCount))
    }

    it("should divide multiple rasters") { 
      val hundreds = RasterHadoopRDD.toRasterRDD(allHundreds.path, sc)
      val twos = RasterHadoopRDD.toRasterRDD(allTwos.path, sc)
      val res = hundreds / twos / twos

      shouldBe(res, (25, 25, allHundreds.tileCount))
    }
  }
}