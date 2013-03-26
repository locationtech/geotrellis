package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CountPointsSpec extends FunSpec 
                         with ShouldMatchers 
                         with TestServer 
                         with RasterBuilders {
  describe("CountPoints") {
    it("returns a zero raster when empty points") {
      val re = RasterExtent(Extent(0,0,9,10),1,1,9,10)
      val op = CountPoints(Seq[Point[Int]](),re)
      assertEqual(run(op),
                  Array.fill[Int](90)(0))
    }

    it("should return 0 raster if points lie outside extent") {
      val re = RasterExtent(Extent(0,0,9,10),1,1,9,10)
      val points = Seq(Point(100,200,0),
                       Point(-10,-30,0),
                       Point(-310,1200,0))
      val op = CountPoints(points,re)
      assertEqual(run(op),
                  Array.fill[Int](90)(0))
    }

    it("counts the points when they are all bunched up in one cell") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val points = Seq(Point(41,59,0),
                       Point(42,58,0),
                       Point(43,57,0),
                       Point(44,56,0),
                       Point(45,58,0))
      val op = CountPoints(points,re)
      
      val expected = Array.fill[Int](90)(0)
      expected(4*9 + 4) = 5

      assertEqual(run(op),
                  expected)
    }

    it("gets counts in the right cells for multiple values") {
      val re = RasterExtent(Extent(0,0,90,100),10,10,9,10)
      val points = for(i <- 0 to 8) yield { 
        Point(10*i + 1, /* ith col */
              100 - ((90 - 10*i) - 1), /* (10-i)'th col */
              0) }

      val op = CountPoints(points,re)
      val data = IntArrayRasterData.ofDim(9,10)
      for(i <- 0 to 8) {
        data.set(i,10 - (i+2),1)
      }

      assertEqual(run(op),
                  Raster(data,re))
    }
  }
}
