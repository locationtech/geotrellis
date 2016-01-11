package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._
import geotrellis.raster.op.zonal.summary._

import geotrellis.vector._

import org.scalatest.FunSpec

class MaxSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers {

  describe("Max Zonal Summary Operation") {
    val inc = IncreasingTestFile

    val tileLayout = inc.metaData.tileLayout
    val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = inc.metaData.extent

    it("should get correct max over whole raster extent") {
      inc.zonalMax(totalExtent.toPolygon) should be(count - 1)
    }

    it("should get correct max over a quarter of the extent") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val result = inc.zonalMax(quarterExtent.toPolygon)
      val expected = inc.stitch.tile.zonalMax(totalExtent, quarterExtent.toPolygon)

      result should be (expected)
    }
  }
}
