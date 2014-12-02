package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._

import geotrellis.vector._

import org.scalatest.FunSpec

class MeanSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {

  describe("Mean Zonal Summary Operation") {
    ifCanRunSpark {
      val inc = IncreasingTestFile

      val tileLayout = inc.metaData.tileLayout
      val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
      val totalExtent = inc.metaData.extent

      it("should get correct mean over whole raster extent") {
        inc.zonalMean(totalExtent.toPolygon) should be((count - 1) / 2.0)
      }

      it("should get correct mean over a quarter of the extent") {
        val xd = totalExtent.xmax - totalExtent.xmin
        val yd = totalExtent.ymax - totalExtent.ymin

        val quarterExtent = Extent(
          totalExtent.xmin,
          totalExtent.ymin,
          totalExtent.xmin + xd / 2,
          totalExtent.ymin + yd / 2
        )

        val min = count / 2

        var sum = 0L
        for (i <- 0 until tileLayout.tileRows * 3) {
          for (j <- 0 until (tileLayout.tileCols * 1.5).toInt) {
            sum += min + i * tileLayout.tileCols * 3 + j
          }
        }

        val res = sum / (count / 4.0)

        inc.zonalMean(quarterExtent.toPolygon) should be(res)
      }
    }
  }

}
