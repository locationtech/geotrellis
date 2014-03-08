package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class MeanSpec extends ZonalSummarySpec {
  describe("Mean") {
    it("computes Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val meanOp = rData.zonalMean(zone)
      run(meanOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes sum on raster source and 5 edge polygon") {
      val vals =
        containedCells
          .map { case (col,row) => tiledR.getDouble(col,row) }
      val sum =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + b }
      val count =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + 1.0 }

      run(tiledRS.zonalMean(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum / count)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes Double Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val meanDOp = rData.zonalMeanDouble(zone)
      run(meanDOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes mean on double raster source and 5 edge polygon") {
      val vals =
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
      val sum =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + b }
      val count =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + 1.0 }

      run(tiledRSDouble.zonalMeanDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum / count.toDouble)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
