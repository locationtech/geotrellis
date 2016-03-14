package geotrellis.spark.summary.polygonal

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._
import geotrellis.raster.summary.polygonal._

import geotrellis.vector._

import org.scalatest.FunSpec

class MinDoubleSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Min Double Zonal Summary Operation") {
    val inc = IncreasingTestFile

    val tileLayout = inc.metadata.tileLayout
    val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = inc.metadata.extent

    it("should get correct double min over whole raster extent") {
      inc.polygonalMinDouble(totalExtent.toPolygon) should be(0)
    }

    it("should get correct double min over a quarter of the extent") {
      val xd = totalExtent.xmax - totalExtent.xmin
      val yd = totalExtent.ymax - totalExtent.ymin

      val quarterExtent = Extent(
        totalExtent.xmin,
        totalExtent.ymin,
        totalExtent.xmin + xd / 2,
        totalExtent.ymin + yd / 2
      )

      val result = inc.polygonalMinDouble(quarterExtent.toPolygon)
      val expected = inc.stitch.tile.polygonalMinDouble(totalExtent, quarterExtent.toPolygon)

      result should be (expected)
    }
  }
}
