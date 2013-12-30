package geotrellis.source

import geotrellis._
import geotrellis.testutil._
import geotrellis.process._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics._

class RasterSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  def getRasterSource = 
    RasterSource("mtsthelens_tiled_cached")

  def getSmallRasterSource =
    RasterSource("quad_tiled")

  describe("convert") {
    it("converts an integer RasterSource to a double RasterSource") {
      val rs = RasterSource(createRaster(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4))
      rs.convert(TypeDouble).get.rasterType should be (TypeDouble)
    }
  }

  describe("RasterSource") {
    it("should load a tiled raster with a target extent") {
      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,_,_) =
        RasterSource("mtsthelens_tiled")
          .info
          .map(_.rasterExtent)
          .get

      val newRe = 
        RasterExtent(
          Extent(xmin,ymax-(ch*256),xmin+(cw*256),ymax),
          cw,ch,256,256)

      val uncropped = RasterSource("mtsthelens_tiled").get
      val cropped = RasterSource("mtsthelens_tiled",newRe).get

      for(row <- 0 until 256) {
        for(col <- 0 until 256) {
          withClue(s"Failed at ($col,$row)") {
            uncropped.get(col,row) should be (cropped.get(col,row))
          }
        }
      }
    }

    it("should converge a tiled raster") {
      val s = 
        RasterSource("mtsthelens_tiled_cached")
          .renderPng

      get(s)

    }

    it("should return a RasterSource when possible") { 
      val d1 = getRasterSource

      val d2:RasterSource = d1.localAdd(3)
      val d3:RasterSource  = d2 mapOp(local.Add(_, 3))
      val d4:RasterSource = d3 map(r => r.map(z => z + 3))
      val d5:DataSource[Int,Seq[Int]] = d3 map(r => r.findMinMax._2)
      
      val result1 = get(d1)
      val result2 = get(d2)
      val result3 = get(d3)
      val result4 = get(d4)
      val result5 = get(d5)

      result1.get(100,100) should be (3233)
      result2.get(100,100) should be (3236)
      result3.get(100,100) should be (3239)
      result4.get(100,100) should be (3242)
      result5.head should be (6026)
    }

    it("should return a RasterSource when calling .distribute") {
      val d1:RasterSource = (getRasterSource + 3).distribute
    }

    it("should handle a histogram result") {
      val d = getRasterSource

      val hist = d.tileHistograms
      val hist2:DataSource[Histogram,Histogram] = d.map( (h:Raster) => FastMapHistogram() )
      case class MinFromHistogram(h:Op[Histogram]) extends Op1(h)({
        (h) => Result(h.getMinValue)
      })

      case class FindMin(ints:Op[Seq[Int]]) extends Op1(ints)({
        (ints) => Result(ints.reduce(math.min(_,_)))
      })

      val ints:DataSource[Int,Seq[Int]] = hist.mapOp(MinFromHistogram(_))
     
      val seqIntVS:ValueSource[Seq[Int]] = ints.converge

      val intVS:ValueSource[Int] = seqIntVS.map( seqInt => seqInt.reduce(math.min(_,_)))
      val intVS2 = ints.reduce(math.min(_,_))

      val histogramResult = get(hist)
      val intsResult = get(ints)

      val intResult = get(intVS)
      val directIntResult = get(intVS2)

      histogramResult.getMinValue should be (2231)
      histogramResult.getMaxValue should be (8367)
      intsResult.length should be (12)
      intResult should be (directIntResult)
      
    }

    it("should handle combine") {
      val d = getRasterSource
      val d2 = getRasterSource

      val combineDS:RasterSource = d.localCombine(d2)(_+_)
      val initial = get(d)
      val result = get(combineDS)

      result.get(3,3) should be (initial.get(3,3) * 2)
    }
  }

  describe("warp") {
    it("should warp with crop only on single tile") {
      // val rs = createRasterSource(
      //   Array( 1,10,100,1000,2,2,2,2,2,
      //          2,20,200,2000,2,2,2,2,2,
      //          3,30,300,3000,2,2,2,2,2,
      //          4,40,400,4000,2,2,2,2,2),
      //   1,1,9,4)

      val rs = RasterSource(createRaster(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4))


      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,cols,rows) = rs.rasterExtent.get
      val newRe = RasterExtent(Extent(xmin,ymax - (ch*3),xmin + (cw*4), ymax),4,3)
      rs.warp(newRe).run match {
        case Complete(r,_) =>
          assertEqual(r,Array(1,10,100,1000,
                              2,20,200,2000,
                              3,30,300,3000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }

    it("should warp with crop only with tiles") {
      val rs = createRasterSource(
        Array( 1,10,100, 1000,2,2, 2,2,2,
               2,20,200, 2000,2,2, 2,2,2,

               3,30,300, 3000,2,2, 2,2,2,
               4,40,400, 4000,2,2, 2,2,2),
        3,2,3,2)

      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,cols,rows) = rs.rasterExtent.get
      val newRe = RasterExtent(Extent(xmin,ymax - (ch*3),xmin + (cw*4), ymax),4,3)
      rs.warp(newRe).run match {
        case Complete(r,_) =>
          assertEqual(r,Array(1,10,100,1000,
                              2,20,200,2000,
                              3,30,300,3000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }
  }
}
