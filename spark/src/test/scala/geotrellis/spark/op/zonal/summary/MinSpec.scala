package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._

import geotrellis.vector._

import org.scalatest.FunSpec

class MinSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {

  describe("Min Zonal Summary Operation") {

    ifCanRunSpark {

      val inc = IncreasingTestFile

      val tileLayout = inc.metaData.tileLayout
      val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
      val totalExtent = inc.metaData.extent

      it("should get correct min over whole raster extent") {
        inc.zonalMin(totalExtent.toPolygon) should be(0)
      }

      it("should get correct min over a quarter of the extent") {
        val xd = totalExtent.xmax - totalExtent.xmin
        val yd = totalExtent.ymax - totalExtent.ymin

        val quarterExtent = Extent(
          totalExtent.xmin,
          totalExtent.ymin,
          totalExtent.xmin + xd / 2,
          totalExtent.ymin + yd / 2
        )
        inc.zonalMin(quarterExtent.toPolygon) should be(count / 2)
      }
    }
  }

}
