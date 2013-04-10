package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class SlopeAspectTests extends FunSpec with ShouldMatchers {
  val server = TestServer.server

  describe("SurfacePoint") {
    it("should calculate trig values correctly") {
      val tolerance = 0.0000000001
      for(x <- Seq(-1.2,0,0.1,2.4,6.4,8.1,9.8)) {
        for(y <- Seq(-11,-3.2,0,0.3,2.65,3.9,10.4,8.11)) {
          val s = new SurfacePoint
          s.`dz/dx` = x
          s.`dz/dy` = y
          val aspect = s.aspect
          val slope = s.slope
          
          if(!slope.isNaN) {
            abs(s.cosSlope - cos(slope)) should be < tolerance
            abs(s.sinSlope - sin(slope)) should be < tolerance
          }
          if(!aspect.isNaN) {
            abs(s.sinAspect - sin(aspect)) should be < tolerance
            abs(s.cosAspect - cos(aspect)) should be < tolerance
          }
        }
      }
    }
  }
}

