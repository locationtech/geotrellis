package geotrellis.spark.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

import geotrellis.vector._

import org.scalatest.FunSpec

import collection._

class StatsTileLayerRDDMethodsSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("RDD Stats Method Operations") {

    it("gives correct class breaks for example raster histogram") {
      val rdd = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          1, 1, 1,  1, 1, 1,  1, 1, 1,
          1, 1, 1,  1, 1, 1,  1, 1, 1,

          2, 2, 2,  2, 2, 2,  2, 2, 2,
          2, 2, 2,  2, 2, 2,  2, 2, 2,

          3, 3, 3,  3, 3, 3,  3, 3, 3,
          3, 3, 3,  3, 3, 3,  3, 3, 3,

          4, 4, 4,  4, 4, 4,  4, 4, 4,
          4, 4, 4,  4, 4, 4,  4, 4, 4), 9, 8),
        TileLayout(3, 4, 3, 2)
      )

      val classBreaks = rdd.classBreaksDouble(4)

      classBreaks should be (Array(1.0, 2.0, 3.0, 4.0))
    }

    it("should find integer min/max of AllOnesTestFile") {
      val ones: TileLayerRDD[SpatialKey] = AllOnesTestFile
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

      val rdd = createTileLayerRDD(sc, tile, tileLayout)

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

      val rdd = createTileLayerRDD(sc, tile, tileLayout)

      val (min, max) = rdd.minMaxDouble

      min should be (-4.1)
      max should be (4.1)
    }

    it ("should find double histogram of aspect and match merged quantile breaks") {
      val path = "raster-test/data/aspect.tif"
      val gt = SinglebandGeoTiff(path)
      val originalRaster = gt.raster.resample(500, 500)
      val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)

      val hist = rdd.histogram
      val hist2 = rdd.histogram

      hist.merge(hist2).quantileBreaks(70) should be (hist.quantileBreaks(70))
    }
  }
}
