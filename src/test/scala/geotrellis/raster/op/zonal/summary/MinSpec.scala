package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class MinSpec extends ZonalSummarySpec {
  describe("Min") {
    it("computes Minimum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val minOp = rData.zonalMin(zone)
      run(minOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes min on raster source and 5 edge polygon") {
      val min =
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(Int.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

      run(tiledRS.zonalMin(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (min)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes Double Minimum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val minDOp = rData.zonalMinDouble(zone)
      run(minDOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes min on double raster source and 5 edge polygon") {
      val min =
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(Double.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

      run(tiledRSDouble.zonalMinDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (min)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
