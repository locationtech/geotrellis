package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
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
      getSquareStdResult(MockCursor.fromAll(NODATA,NODATA,NODATA,NODATA)).isNaN should be (true)
    }

    it("should match calculated std on default sets") {
      for(s <- defaultTestSets) {
        val sf = s.filter { x => x != NODATA }.toList
        val xs = sf
        val μ = mean(xs)
        val σ = stddev(xs, μ)
        if(σ.isNaN) {
          getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]())).isNaN should equal (true)
        } else {
          getSquareStdResult(MockCursor.fromAddRemoveAll(s,s,Seq[Int]())) should equal (σ)
        }
      }
    }
  }
}
