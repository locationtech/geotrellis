package geotrellis.spark.op.elevation

import geotrellis.raster._
import geotrellis.raster.op.elevation._

import geotrellis.spark._

import geotrellis.vector.Extent

import org.scalatest._

import spire.syntax.cfor._

class HillshadeSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders
    with OpAsserter {

  describe("Hillshade Elevation Spec") {

    ifCanRunSpark {

      it("should get the same result on elevation for spark op as single raster op") {
        val rasterOp = (tile: Tile, re: RasterExtent) => tile.hillshade(re.cellSize)
        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.hillshade()

        val path = "elevation.json"

        testArg(sc, path)(rasterOp, sparkOp)
      }

      it("should get same result on SBN_inc_percap for spark op as single raster op") {
        val rasterOp = (tile: Tile, re: RasterExtent) => tile.hillshade(re.cellSize)
        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.hillshade()

        val path = "SBN_inc_percap.json"

        val asserter = (ta: Tile, tb: Tile) => {
          val cols = math.min(ta.cols, tb.cols)
          val rows = math.min(ta.rows, tb.rows)

          cfor(1)(_ < rows - 1, _ + 1) { row =>
            cfor(1)(_ < cols - 1, _ + 1) { col =>
              withClue (s"different at $col, $row: ") {
                val v1 = tb.getDouble(col, row)
                val v2 = ta.getDouble(col, row)
                if (isNoData(v1)) isNoData(v2) should be (true)
                else if (isNoData(v2)) isNoData(v1) should be (true)
                else v1 should be (v2)
              }
            }
          }
        }

        testArg(sc, path)(rasterOp, sparkOp, asserter)
      }

    }

  }
}
