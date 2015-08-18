package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._
import geotrellis.raster.op.zonal.summary._

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
      val totalExtent = inc.metaData.dataExtent

      it("should get correct mean over whole raster extent") {
        inc.zonalMean(totalExtent.toPolygon) should be((count - 1) / 2.0)
      }

      it("should get correct mean over a quarter of the extent") {
        val xd = totalExtent.width
        val yd = totalExtent.height

        val quarterExtent = Extent(
          totalExtent.xmin,
          totalExtent.ymin,
          totalExtent.xmin + xd / 2,
          totalExtent.ymin + yd / 2
        )
        val result = inc.zonalMean(quarterExtent.toPolygon)
        val expected = inc.stitch.zonalMean(totalExtent, quarterExtent.toPolygon)

        result should be (expected)
      }
    }
  }

}
