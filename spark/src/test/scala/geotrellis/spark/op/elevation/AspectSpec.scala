package geotrellis.spark.op.elevation

import geotrellis.spark._

import geotrellis.raster.op.elevation._
import geotrellis.raster._

import org.scalatest.FunSpec

class AspectSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders
    with OpAsserter {

  describe("Aspect Elevation Spec") {

    ifCanRunSpark {

      it("should match gdal computed slope raster") {
        val rasterOp = (tile: Tile, re: RasterExtent) => tile.aspect(re.cellSize)
        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.aspect()

        val path = "elevation.json"

        testArg(sc, path)(rasterOp, sparkOp)
      }

    }
  }
}
