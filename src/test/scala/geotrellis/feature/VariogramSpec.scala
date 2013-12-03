package geotrellis.feature

import geotrellis._
import geotrellis.feature.Variogram
// import geotrellis.raster._
// import geotrellis.io._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner
import geotrellis.testutil._


import org.apache.commons.math3.stat.regression.SimpleRegression

object VariogramSpec {
  def trunc(n:Double, p:Int):Double = { 
    val s = math.pow(10,p)
    math.floor(n*s) / s
  }
}

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class VariogramSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer 
                            with RasterBuilders {
  describe("Variogram") {
    it("Simple Regression Test 1") {
      val regression = new SimpleRegression
      val points = Seq[(Double,Double)](
        (1.47,52.21),
        (1.50,53.12),
        (1.52,54.48),
        (1.55,55.84),
        (1.57,57.20),
        (1.60,58.57),
        (1.63,59.93),
        (1.65,61.29),
        (1.68,63.11),
        (1.70,64.47),
        (1.73,66.28),
        (1.75,68.10),
        (1.78,69.92),
        (1.80,72.19),
        (1.83,74.46))

      for((x,y) <- points) { regression.addData(x,y) }
      val slope = VariogramSpec.trunc(regression.getSlope,3)
      val intercept = VariogramSpec.trunc(regression.getIntercept,3)

      val slopeExpected = 61.272
      val interceptExpected = -39.061
      withClue(s"Slope & Intercept Test:") {
        (slope,intercept) should be (slopeExpected,interceptExpected)
      }
    }

    it("Simple Regression Test 2") {
      val regression = new SimpleRegression
      val points = Seq[(Double,Double)]((1,2),(2,3))

      for((x,y) <- points) { regression.addData(x,y) }
      val slope = VariogramSpec.trunc(regression.getSlope,3)
      val intercept = VariogramSpec.trunc(regression.getIntercept,3)

      val slopeExpected = 1.000
      val interceptExpected = 1.000
      withClue(s"Slope & Intercept Test:") {
        (slope,intercept) should be (slopeExpected,interceptExpected)
      }
    }
  }
}
