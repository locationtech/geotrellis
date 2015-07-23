package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster.op.global._
import geotrellis.raster._

import org.scalatest.FunSpec

class VerticalFlipSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders
    with OpAsserter {

  describe("VerticalFlip Global Spec") {

    ifCanRunSpark {

      it("should perform as the non-distributed raster operation") {
        val rasterOp = (tile: Tile, re: RasterExtent) => tile.verticalFlip
        val sparkOp = (rdd: RasterRDD[SpatialKey, Tile]) => rdd.verticalFlip

        val path = "aspect.tif"

        testGeoTiff(sc, path)(rasterOp, sparkOp)
      }

    }
  }
}
