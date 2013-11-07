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
  val tiledRS = 
    createRasterSource(
      Array(  1, 2, 3,   4, 5, 6,   7, 8, 9,
              1, 2, 3,   4, 5, 6,   7, 8, 9,

             10,11,12,  13,nd,14,  nd,15,16,
             11,12,13,  nd,15,nd,  17,18,19
      ),
      3,2,3,2)

  val tiledRSDouble = 
    createRasterSource(
      Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
              0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

             0.11,0.12,0.13,   0.14,NaN,0.15,   NaN, 0.16,0.17,
             0.11,0.12,0.13,    NaN,0.15,NaN,  0.17, 0.18,0.19
      ),
      3,2,3,2)


  val tiledR = runSource(tiledRS)
  val tiledRDouble = runSource(tiledRSDouble)

  val poly = {
    val re = tiledR.rasterExtent
    val polyPoints = Seq(
      re.gridToMap(2,1), re.gridToMap(4,0),re.gridToMap(7,2),
      re.gridToMap(5,3), re.gridToMap(2,2),re.gridToMap(2,1)
    )
    Polygon(polyPoints, 0)
  }

  val containedCells = Seq(
//         (4,0),
    (3,1),(4,1),(5,1),
    (3,2),(4,2),(5,2),(6,2)
//                (5,3)
  )
  
//      Polygon vertices are 0's (not contianed cells)
//      X's are contained cells
// 
//       *  *  *    *  0  *    *  *  *
//       *  *  0    X  X  X    *  *  *
// 
//       *  *  0    X  X  X    X  0  *
//       *  *  *    *  *  0    *  *  *  
// 

//   I'm not entirely sure if the cell centers should be excluded.

  describe("test case") {
    it("is as we think. Make sure we understand what cells are contained in the poly") {
      val re = tiledR.rasterExtent
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          val p = Point(re.gridColToMap(col),re.gridRowToMap(row),0)
          if(containedCells.contains((col,row))) {
            withClue(s"$col,$row should be not be included") {
//              poly.geom.intersects(p.geom) should be (true)
              poly.geom.contains(p.geom) should be (true)
            }
          } else {
            withClue(s"$col,$row should be included") {
//              poly.geom.intersects(p.geom) should be (false)
              poly.geom.contains(p.geom) should be (false)
            }
          }
        }
      }
    }
  }

  describe("zonalSum") {
    it("computes Sum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("fails on nonintersecting zone") {
      // Test non-intersecting polygons (issue #412)
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes sum on raster source and 5 edge polygon") {
      val sum = 
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(0) { (a,b) => if(b == NODATA) a else a + b }

      getSource(tiledRS.zonalSum(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalSumDouble") {
    it("computes Double Sum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes Double sum on raster source and 5 edge polygon") {
      val sum = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + b }

      getSource(tiledRSDouble.zonalSumDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMin") {
    it("computes Minimum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes min on raster source and 5 edge polygon") {
      val min = 
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(Int.MaxValue) { (a,b) => if(b == NODATA) a else math.min(a, b) }

      getSource(tiledRS.zonalMin(poly)) match {
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

  describe("zonalMinDouble") {
    it("computes Double Minimum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes min on double raster source and 5 edge polygon") {
      val min = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(Double.MaxValue) { (a,b) => if(isNaN(b)) a else math.min(a, b) }

      getSource(tiledRSDouble.zonalMinDouble(poly)) match {
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

  describe("zonalMax") {
    it("computes Maximum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes max on raster source and 5 edge polygon") {
      val max = 
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(Int.MinValue) { (a,b) => if(b == NODATA) a else math.max(a, b) }

      getSource(tiledRS.zonalMax(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (max)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMaxDouble") {
    it("computes Double Maximum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes max on double raster source and 5 edge polygon") {
      val max = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(Double.MinValue) { (a,b) => if(isNaN(b)) a else math.max(a, b) }

      getSource(tiledRSDouble.zonalMaxDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (max)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalHistogram") {
    it("computes Histogram") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes Histogram for raster source and 5 edge polygon") {
      val h = statistics.FastMapHistogram()

      for(z <- containedCells.map { case (col,row) => tiledR.get(col,row) }) {
        if (z != NODATA) { h.countItem(z, 1) }
      }

      getSource(tiledRS.zonalHistogram(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          for(row <- 0 until tiledR.rows) {
            for(col <- 0 until tiledR.cols) {
              val v = tiledR.get(col,row)
              if(v != NODATA) {
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

  describe("zonalMean") {
    it("computes Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes sum on raster source and 5 edge polygon") {
      val vals = 
        containedCells
          .map { case (col,row) => tiledR.getDouble(col,row) }
      val sum = 
        vals
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + b }
      val count = 
        vals
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + 1.0 }

      getSource(tiledRS.zonalMean(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum / count)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMeanDouble") {
    it("computes Double Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
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

    it("computes Double sum on raster source and 5 edge polygon") {
      val sum = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + b }

      getSource(tiledRSDouble.zonalSumDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum)
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
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + b }
      val count = 
        vals
          .foldLeft(0.0) { (a,b) => if(isNaN(b)) a else a + 1.0 }

      getSource(tiledRSDouble.zonalMeanDouble(poly)) match {
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
