package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner
import scala.math._

@RunWith(classOf[JUnitRunner])
class AspectSpec extends FunSpec with ShouldMatchers
                                 with TestServer {
  describe("Aspect") {
    it("should match gdal computed aspect raster") {
      val rOp = get("elevation")
      val gdalOp = get("aspect")
      val aspectComputed = Aspect(rOp)
      val r = run(gdalOp)
      val r2 = run(aspectComputed)
      var x = 0
      var y = 0
      while(y < r.rows - 1) {
        x = 0
        while(x < r.cols - 1) {
          val rv = r.getDouble(x,y)
          val r2v = r2.getDouble(x,y)
          if(rv.isNaN && !r2v.isNaN) {
            println(s" Happening at (${x},${y}) = gdal: ${rv}   us: ${r2v}")
          }
          x += 1
        }
        y += 1
      }
      assertEqual(gdalOp,aspectComputed)
    }
  }

  describe("Slope") {
    it("should match gdal computed slope raster") {
      val rOp = get("elevation")
      val gdalOp = get("slope")
      val slopeComputed = Slope(rOp,1.0)
      val r = run(gdalOp)
      val r2 = run(slopeComputed)
      var x = 0
      var y = 0
      while(y < r.rows - 1) {
        x = 0
        while(x < r.cols - 1) {
          val rv = r.getDouble(x,y)
          val r2v = r2.getDouble(x,y)
          if(rv.isNaN && !r2v.isNaN) {
            println(s" Happening at (${x},${y}) = gdal: ${rv}   us: ${r2v}")
          }
          x += 1
        }
        y += 1
      }
      assertEqual(gdalOp,slopeComputed)
    }
  }
}
