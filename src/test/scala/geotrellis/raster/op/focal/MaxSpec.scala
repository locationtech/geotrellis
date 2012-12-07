package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class MaxSpec extends FunSpec with ShouldMatchers 
                              with TestServer 
                              with RasterBuilders {
  describe("Max") {
    it("should agree with a manually worked out example") {
      val r = createRaster(Array[Int](1,1,1,1,
                                      2,2,2,2,
                                      3,3,3,3,
                                      1,1,4,4))

      val maxOp = focal.Max(r,Square(1))
      assertEqual(maxOp, Array[Int](2,2,2,2,
                                    3,3,3,3,
                                    3,4,4,4,
                                    3,4,4,4))
    }

    it("should handle NODATA") {
      val r = createRaster(Array[Int](1,NODATA,1,NODATA,
                                      NODATA,NODATA,NODATA,NODATA,
                                      NODATA,NODATA,NODATA,NODATA,
                                      NODATA,NODATA,NODATA,200))
      val maxOp = focal.Max(r,Square(1))
      assertEqual(maxOp,Array[Int](1,1,1,1,
                                   1,1,1,1,
                                   NODATA,NODATA,200,200,
                                   NODATA,NODATA,200,200))
    }
  }
}
