package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class HistogramSpec extends ZonalSummarySpec {
  describe("zonalHistogram") {
    it("computes Histogram") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val histOp = rData.zonalHistogram(zone)
      run(histOp) match {
        case Complete(result,success) =>
          //           println(success)
          result.getItemCount(1) should equal (40)
          result.getItemCount(2) should equal (0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes Histogram for raster source and 5 edge polygon") {
      val h = statistics.FastMapHistogram()

      for(z <- containedCells.map { case (col,row) => tiledR.get(col,row) }) {
        if (isData(z)) { h.countItem(z, 1) }
      }

      run(tiledRS.zonalHistogram(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          for(row <- 0 until tiledR.rows) {
            for(col <- 0 until tiledR.cols) {
              val v = tiledR.get(col,row)
              if(isData(v)) {
                result.getItemCount(v) should be (h.getItemCount(v))
              }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
