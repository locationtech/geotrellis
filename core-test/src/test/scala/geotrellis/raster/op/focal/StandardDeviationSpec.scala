package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import scala.math._

class StandardDeviationSpec extends FunSpec with FocalOpSpec
                                            with ShouldMatchers {

  val getCircleStdResult = (getDoubleCursorResult _).curried((r,n) => StandardDeviation(r,n))(Circle(1))
  val getSquareStdResult = (getDoubleCursorResult _).curried((r,n) => StandardDeviation(r,n))(Square(1))

  def mean(xs: List[Int]): Double = xs match {
    case Nil => Double.NaN
    case ys => ys.reduceLeft(_ + _) / ys.size.toDouble
  }
                                              
  def stddev(xs: List[Int], avg: Double): Double = xs match {
    case Nil => Double.NaN
    case ys => math.sqrt((0.0 /: ys) {
      (a,e) => a + math.pow(e - avg, 2.0)
    } / xs.size.toDouble)
  }

  describe("StandardDeviation") {
    it("should handle all NODATA") {
      isNoData(getSquareStdResult(MockCursor.fromAll(NODATA,NODATA,NODATA,NODATA))) should be (true)
    }

    it("should match calculated std on default sets") {
      for(s <- defaultTestSets) {
        val sf = s.filter { x => isData(x) }.toList
        val xs = sf
        val μ = mean(xs)
        val σ = stddev(xs, μ)
        if(isNoData(σ)) {
          isNoData(getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]()))) should equal (true)
        } else {
          getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]())) should equal (σ)
        }
      }
    }
  }
}
