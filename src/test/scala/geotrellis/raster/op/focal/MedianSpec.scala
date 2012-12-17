package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
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

      var result = run(Median(r,Square(1)))

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
  }
}
