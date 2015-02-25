package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class LocalMapSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Local Map Operations") {
    ifCanRunSpark {
      val ones = AllOnesTestFile
      val twos = AllTwosTestFile
      val hundreds = AllHundredsTestFile
      val inc = IncreasingTestFile
      val dec = DecreasingTestFile

      val (cols: Int, rows: Int) = {
        val tile = ones.stitch
        (tile.cols, tile.rows)
      }


      it("should map an integer function over an integer raster rdd") {
        val result = ones.localMap(z => z + 1)
        rasterShouldBe(result, (x: Int, y: Int) => 2)
        rastersShouldHaveSameIdsAndTileCount(ones, result)
      }

    }
  }
}
