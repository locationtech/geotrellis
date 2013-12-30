package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class ZonalSummaryOpMethodsSpec extends FunSpec
                                   with ShouldMatchers
                                   with TestServer
                                   with RasterBuilders {
  
  // describe("zonalMin") {
  //   it("computes Minimum") {
  //     val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
  //     val zone = Extent(10,-10,30,10).asFeature()

  //     val minOp = rData.zonalMin(zone)
  //     run(minOp) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (1)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }

  //   it("computes min on raster source and 5 edge polygon") {
  //     val min = 
  //       containedCells
  //         .map { case (col,row) => tiledR.get(col,row) }
  //         .foldLeft(Int.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

  //     run(tiledRS.zonalMin(poly)) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (min)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }
  // }

  // describe("zonalMinDouble") {
  //   it("computes Double Minimum") {
  //     val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
  //     val zone = Extent(10,-10,30,10).asFeature()

  //     val minDOp = rData.zonalMinDouble(zone)
  //     run(minDOp) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (1.0)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }

  //   it("computes min on double raster source and 5 edge polygon") {
  //     val min = 
  //       containedCells
  //         .map { case (col,row) => tiledRDouble.getDouble(col,row) }
  //         .foldLeft(Double.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

  //     run(tiledRSDouble.zonalMinDouble(poly)) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (min)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }
  // }

  // describe("zonalMax") {
  //   it("computes Maximum") {
  //     val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
  //     val zone = Extent(10,-10,30,10).asFeature()

  //     val maxOp = rData.zonalMax(zone)
  //     run(maxOp) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (1)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }

  //   it("computes max on raster source and 5 edge polygon") {
  //     val max = 
  //       containedCells
  //         .map { case (col,row) => tiledR.get(col,row) }
  //         .foldLeft(Int.MinValue) { (a,b) => if(isNoData(b)) a else math.max(a, b) }

  //     run(tiledRS.zonalMax(poly)) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (max)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }
  // }

  // describe("zonalMaxDouble") {
  //   it("computes Double Maximum") {
  //     val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
  //     val zone = Extent(10,-10,30,10).asFeature()

  //     val maxDOp = rData.zonalMaxDouble(zone)
  //     run(maxDOp) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (1.0)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }

  //   it("computes max on double raster source and 5 edge polygon") {
  //     val max = 
  //       containedCells
  //         .map { case (col,row) => tiledRDouble.getDouble(col,row) }
  //         .foldLeft(Double.MinValue) { (a,b) => if(isNoData(b)) a else math.max(a, b) }

  //     run(tiledRSDouble.zonalMaxDouble(poly)) match {
  //       case Complete(result,success) =>
  //         //           println(success)
  //         result should equal (max)
  //       case Error(msg,failure) =>
  //         println(msg)
  //         println(failure)
  //         assert(false)
  //     }
  //   }
  // }

  describe("ZonalSummaryOpMethods") {
    it("computes mean on double raster source with cached results") {
      val rs =
        createRasterSource(
          Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.11,0.12,0.13,   0.14,NaN,0.15,   NaN, 0.16,0.17,
            0.11,0.12,0.13,    NaN,0.15,NaN,  0.17, 0.18,0.19,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9
          ),
          3,4,3,2)

      val tr = rs.get

      val p = {
        val re = tr.rasterExtent
        val polyPoints = Seq(
          re.gridToMap(1,1),re.gridToMap(2,0), re.gridToMap(4,0),re.gridToMap(7,2),
          re.gridToMap(6,6), re.gridToMap(1,6),re.gridToMap(0,3),re.gridToMap(1,1)
        )
        Polygon(polyPoints, 0)
      }

      val cached =
        rs.map(tile => { println("using cached result") ; MeanResult.fromFullTileDouble(tile)})
          .cached

      val withCached = rs.zonalMeanDouble(p,cached)
      val withoutCached = rs.zonalMeanDouble(p)

      withCached.get should be (withoutCached.get)
    }
  }
}
