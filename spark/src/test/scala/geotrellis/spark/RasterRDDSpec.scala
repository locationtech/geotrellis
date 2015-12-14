package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class RasterRDDSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with RasterRDDBuilders
    with TestSparkContext {
  describe("RasterRDD") {
    it("should find integer min/max of AllOnesTestFile") {
      val ones: RasterRDD[SpatialKey] = AllOnesTestFile
      val (min, max) = ones.minMax

      min should be (1)
      max should be (1)
    }

    it ("should find integer min/max of example") {
      val arr: Array[Int] =
        Array(1, 1, 2, 2,
          3, 3, 4, 4,

          -1, -1, -2, -2,
          -3, -3, -4, -4)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createRasterRDD(sc, tile, tileLayout)

      val (min, max) = rdd.minMax

      min should be (-4)
      max should be (4)
    }

    it ("should find double min/max of example") {
      val arr: Array[Double] =
        Array(1, 1, 2, 2,
          3, 3, 4.1, 4.1,

          -1, -1, -2, -2,
          -3, -3, -4.1, -4.1)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createRasterRDD(sc, tile, tileLayout)

      val (min, max) = rdd.minMaxDouble

      min should be (-4.1)
      max should be (4.1)
    }

  }
}
