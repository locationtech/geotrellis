package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.source._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import scalaxy.loops._
import scala.math._

class MedianSpec extends FunSpec with TestServer
                                 with FocalOpSpec
                                 with ShouldMatchers {

  describe("Median") {
    it("should match worked out results") {
      val r = createRaster(Array(3, 4, 1, 1, 1,
                                 7, 4, 0, 1, 0,
                                 3, 3, 7, 7, 1,
                                 0, 7, 2, 0, 0,
                                 6, 6, 6, 5, 5))

      var result = get(Median(r,Square(1)))

      def median(s:Int*) = {
        if(s.length % 2 == 0) {
        val middle = (s.length/2) -1          
          (s(middle) + s(middle+1)) / 2         
        } else {
          s(s.length/2)
        }
      }

      result.get(0,0) should equal (median(2,4,4,7))
      result.get(1,0) should equal (median(0,1,3,4,4,7))
      result.get(2,0) should equal (median(0,1,1,1,4,4))
      result.get(3,0) should equal (median(0,0,1,1,1,1))
      result.get(4,0) should equal (median(0,1,1,1))
      result.get(0,1) should equal (median(3,3,3,4,4,7))
      result.get(1,1) should equal (median(0,1,3,3,3,4,4,7,7))
      result.get(2,1) should equal (median(0,1,1,1,3,4,4,7,7))
      result.get(3,1) should equal (median(0,0,1,1,1,1,1,7,7))
      result.get(4,1) should equal (median(0,1,1,1,1,7))
      result.get(0,2) should equal (median(0,3,3,4,7,7))
      result.get(1,2) should equal (median(0,0,2,3,3,4,7,7,7))
      result.get(2,2) should equal (median(0,0,1,2,3,4,7,7,7))
      result.get(3,2) should equal (median(0,0,0,0,1,1,2,7,7))
      result.get(4,2) should equal (median(0,0,0,1,1,7))
      result.get(0,3) should equal (median(0,3,3,6,6,7))
      result.get(1,3) should equal (median(0,2,3,3,6,6,6,7,7))
      result.get(2,3) should equal (median(0,2,3,5,6,6,7,7,7))
      result.get(3,3) should equal (median(0,0,1,2,5,5,6,7,7))
      result.get(4,3) should equal (median(0,0,1,5,5,7))
      result.get(0,4) should equal (median(0,6,6,7))
      result.get(1,4) should equal (median(0,2,6,6,6,7))
      result.get(2,4) should equal (median(0,2,5,6,6,7))
      result.get(3,4) should equal (median(0,0,2,5,5,6))
      result.get(4,4) should equal (median(0,0,5,5))
    }

    it("should get median on raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,1,      1,3,5,      9,8,2,
                9,1,1,      2,2,2,      4,3,5,

                3,8,1,     3, 3,3,      1,2,2,
                2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMedian(Square(1))) match {
        case Complete(result,success) =>
//          println(success)
          assertEqual(result,
            Array(7, 1, 1,    1, 2, 3,    4, 4, 4,
                  7, 2, 1,    2, 3, 3,    3, 3, 2,

                  3, 3, 2,    2, 2, 2,    3, 3, 3,
                  3, 3, 3,    3, 3, 3,    2, 2, 2))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should run median square 3 on tiled raster in catalog") {
      val name = "SBN_inc_percap"

      val source = RasterSource(name)
      val r = source.get

      val expected = source.focalMedian(Square(3)).get

      val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
      val rs = RasterSource(TileRaster.wrap(r,tileLayout,cropped = false))

      rs.focalMedian(Square(3)).run match {
        case Complete(value,hist) =>
          for(col <- 0 until expected.cols optimized) {
            for(row <- 0 until expected.rows optimized) {
              withClue (s"Value different at $col,$row: ") {
                val v1 = expected.getDouble(col,row)
                val v2 = value.getDouble(col,row)
                if(isNoData(v1)) isNoData(v2) should be (true)
                else if(isNoData(v2)) isNoData(v1) should be (true)
                else v1 should be (v2)
              }
            }
          }
        case Error(message,trace) =>
          println(message)
          assert(false)
      }
    }
  }
}
