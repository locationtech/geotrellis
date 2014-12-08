package geotrellis.spark.op.elevation

import geotrellis.spark._

import geotrellis.raster.op.elevation._
import geotrellis.raster._

import org.scalatest.FunSpec

class SlopeSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders
    with OpAsserter {

  describe("Slope Elevation Spec") {

    ifCanRunSpark {

      it("should match gdal computed slope raster") {
        val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize)
        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.slope()

        val path = "elevation.json"

        testArg(sc, path)(rasterOp, sparkOp)
      }

    }
  }
}
