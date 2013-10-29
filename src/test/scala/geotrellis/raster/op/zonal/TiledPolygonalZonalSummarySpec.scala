package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

// for PartialTile
import scala.language.existentials


// TODO
 @RunWith(classOf[org.scalatest.junit.JUnitRunner])
 class TiledPolygonalZonalSummarySpec extends FunSpec
                                      with ShouldMatchers
                                      with TestServer
                                      with RasterBuilders {
//   sealed trait TestResultTile

//   case class FullTile(re: RasterExtent) extends TestResultTile
//   case class PartialTile(re: RasterExtent, poly: Geometry[_]) extends TestResultTile
//   case object NoDataTile extends TestResultTile

//   object PartialTile {
//     def norm(re: RasterExtent, poly: Geometry[_]) = new PartialTile(re, poly.mapGeom(z => {z.normalize; z}))
//   }
 
//   case class MockTiledPolygonalZonalSummary[DD](
//     r:Op[Raster], zonePolygon:Op[Polygon[DD]])(
//     implicit val mB: Manifest[TestResultTile], val mD: Manifest[DD])
//        extends TiledPolygonalZonalSummary[Seq[TestResultTile]] {

//     type B = TestResultTile
//     type D = DD

//     def handlePartialTileIntersection(r: Op[Raster], p: Op[Geometry[D]]) = {
//       logic.Do(r,p)( (r,p) => PartialTile.norm(r.rasterExtent, p))
//     }

//     def handleFullTile(r: Op[Raster]) = {
//       logic.Do(r)( r => FullTile(r.rasterExtent))
//     }

//     def handleNoDataTile = NoDataTile

//     def reducer(mapResults: List[B]):Seq[B] = mapResults
//   }

   describe("ZonalSummary") {
//     it("zonal summary summarizes one raster") {
//       val rData = new IntArrayRasterData(Array.ofDim[Int](9), 3, 3).map { i => 1 }
//       val extent = Extent(0,0,90,90)
//       val rasterExtent = RasterExtent(extent,30,30,3,3)
//       val r = Raster(rData, rasterExtent)

//       val zone = extent.asFeature(())

//       val summaryOp = MockTiledPolygonalZonalSummary(r,zone)

//       val result = server.run(summaryOp)

//       result should equal (List(FullTile(rasterExtent)))
//     }

     it("should handle tiled stuff") {
//       def getTile(i: Int, data: Int) = {
//         val rData = new IntArrayRasterData(Array.fill[Int](100)(data), 10, 10)
//         val row = i % 4
//         val col = i - (row * 4)
//         val extent = Extent(row*10, col*10, (row+1)*10, (col+1)*10)
//         val rasterExtent = RasterExtent(extent, 1, 1, 10, 10)
//         Raster(rData, rasterExtent)
//       }

//       val tiles = Array[Int](1,2,3,4,5,6,7,8,9,1,2,3,4,5,6,7)
//         .zipWithIndex
//         .map { case (data, i) => getTile(i, data) }

//       val tileLayout = TileLayout(4, 4, 10, 10)

       val rasterExtent = RasterExtent(Extent(0,0,40,40),1,1,40,40)
       val rData = createRasterDataSource(Array.fill(40*40)(1),4,4,10,10)

//       val raster = new Raster(rData, rasterExtent)

       val zone = Extent(15,10,30,20).asFeature(0)
      
//       val summaryOp = MockTiledPolygonalZonalSummary(raster, zone)
//       val result = server.run(summaryOp).toSet

//       val e = Extent(15,10,20,20).asFeature(()).mapGeom(a => {a.normalize(); a})

//       Set(FullTile(RasterExtent(Extent(20,10,30,20),1,1,10,10)),
//           PartialTile(RasterExtent(Extent(10,10,20,20),1,1,10,10),e)) should equal (result)

       val sumOp = rData.zonalSum(zone)
       server.getSource(sumOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (250)
         case Error(msg,failure) =>
           println(s"MSG: $msg")
           println(s"FAILURE: $failure")
           assert(false)
       }

       val sumDOp = rData.zonalSumDouble(zone)
       server.getSource(sumDOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (250.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val minOp = rData.zonalMin(zone)
       server.getSource(minOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (1)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val minDOp = rData.zonalMinDouble(zone)
       server.getSource(minDOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (1.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val maxOp = rData.zonalMax(zone)
       server.getSource(maxOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (2)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val maxDOp = rData.zonalMaxDouble(zone)
       server.getSource(maxDOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (2.0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val histOp = rData.zonalHistogram(zone)
       server.getSource(histOp) match {
         case Complete(result,success) =>
           println(success)
           result.getItemCount(1) should equal (50)
           result.getItemCount(2) should equal (100)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val meanOp = rData.zonalMean(zone)
       server.getSource(meanOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (1)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       val meanDOp = rData.zonalMeanDouble(zone)
       server.getSource(meanDOp) match {
         case Complete(result,success) =>
           println(success)
           result should equal (1.6666666666666667)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }

       // Test non-intersecting polygons (issue #412)
       val nonintersecting = Extent(100,120,100,120).asFeature(())
       val sumOp2 = rData.zonalSum(nonintersecting)
       server.getSource(sumOp2) match {
         case Complete(result,success) =>
           println(success)
           result should equal (0)
         case Error(msg,failure) =>
           println(msg)
           println(failure)
           assert(false)
       }
     }
   }
 }
