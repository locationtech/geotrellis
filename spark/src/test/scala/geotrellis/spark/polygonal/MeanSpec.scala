package geotrellis.spark.polygonal

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._
import geotrellis.raster.polygonal._

import geotrellis.vector._

import org.scalatest.FunSpec

class MeanSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Mean Zonal Summary Operation") {
    val inc = IncreasingTestFile

    val tileLayout = inc.metaData.tileLayout
    val count = (inc.count * tileLayout.tileCols * tileLayout.tileRows).toInt
    val totalExtent = inc.metaData.extent

    it("should get correct mean over whole raster extent") {
      inc.polygonalMean(totalExtent.toPolygon) should be((count - 1) / 2.0)
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
      val result = inc.polygonalMean(quarterExtent.toPolygon)
      val expected = inc.stitch.tile.polygonalMean(totalExtent, quarterExtent.toPolygon)

      result should be (expected)
    }
  }
}
