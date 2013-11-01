package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._

 @RunWith(classOf[org.scalatest.junit.JUnitRunner])
 class TiledPolygonalZonalSummarySpec extends FunSpec
                                      with ShouldMatchers
                                      with TestServer
                                      with RasterBuilders {
   describe("ZonalSummary") {
     it("computes Sum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val sumOp = rData.zonalSum(zone)
       getSource(sumOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (40)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Double Sum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val sumDOp = rData.zonalSumDouble(zone)
       getSource(sumDOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (40.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Minimum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val minOp = rData.zonalMin(zone)
       getSource(minOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Double Minimum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val minDOp = rData.zonalMinDouble(zone)
       getSource(minDOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Maximum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val maxOp = rData.zonalMax(zone)
       getSource(maxOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Double Maximum") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val maxDOp = rData.zonalMaxDouble(zone)
       getSource(maxDOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Histogram") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val histOp = rData.zonalHistogram(zone)
       getSource(histOp) match {
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

     it("computes Mean") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val meanOp = rData.zonalMean(zone)
       getSource(meanOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("computes Double Mean") {
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val meanDOp = rData.zonalMeanDouble(zone)
       getSource(meanDOp) match {
         case Complete(result,success) =>
//           println(success)
           result should equal (1.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }

     it("fails on nonintersecting zone") {
       // Test non-intersecting polygons (issue #412)
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)
       val zone = Extent(10,-10,30,10).asFeature()

       val nonintersecting = Extent(100,120,100,120).asFeature(())
       val sumOp2 = rData.zonalSum(nonintersecting)
       getSource(sumOp2) match {
         case Complete(result,success) =>
           assert(false)
         case Error(msg,failure) =>
           msg should equal ("empty.reduceLeft")
       }
     }
   }
 }
