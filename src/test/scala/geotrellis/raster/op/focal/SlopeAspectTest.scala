package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class SlopeAspectTests extends FunSpec with ShouldMatchers {
  val server = TestServer("src/test/resources/catalog.json")

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
          println("Doing (%f,%f)".format(s.`dz/dx`,s.`dz/dy`))
          abs(s.cosSlope - cos(slope)) should be < tolerance
          abs(s.sinSlope - sin(slope)) should be < tolerance

          abs(s.sinAspect - sin(aspect)) should be < tolerance
          println("   cosAspect %f   cos(aspect) %f aspect %f".format(s.cosAspect,cos(aspect),(aspect)))
          abs(s.cosAspect - cos(aspect)) should be < tolerance
        }
      }
    }
  }

  describe("Slope") {
    ignore("calculates loaded raster correctly") {
      val threshold = 3.0 //threshold for equality

      // Expected data, from GDAL
      val gdalComputedSlopeOp = io.LoadRaster("elevation-slope-gdal")

      val elevationOp = io.LoadRaster("elevation")
      val slopeOp = focal.Slope(elevationOp,1.0)

      //Compare the two's values
      val diffOp = local.Subtract(slopeOp, gdalComputedSlopeOp)
      val diff = server.run(diffOp)

      val x = server.run(slopeOp)
      new geotrellis.data.arg.ArgWriter(geotrellis.TypeFloat).write("/home/rob/tmp/elevationop/test-slope.arg", x, "test-slope")

      var maxDiff = threshold
      diff.foreachDouble {
	z => if (z != Double.NaN) {
	  if(abs(z) > maxDiff) { maxDiff = abs(z)}
	}
      }

      maxDiff should be <= (threshold)
    }
  }

  describe("Aspect") {
    ignore("calculates loaded raster correctly") {
      val threshold = 3.0 //threshold for equality

      // Expected data, from GDAL
      val gdalComputedAspectOp = io.LoadRaster("elevation-aspect-gdal")

      val elevationOp = io.LoadRaster("elevation")
      val aspectOp = focal.Aspect(elevationOp)

      //Compare the two's values
      val diffOp = local.Subtract(aspectOp, gdalComputedAspectOp)
      val diff = server.run(diffOp)

      val x = server.run(aspectOp)
      new geotrellis.data.arg.ArgWriter(geotrellis.TypeFloat).write("/home/rob/tmp/elevationop/test-aspect.arg", x, "test-aspect")

      var maxDiff = threshold
      diff.foreachDouble {
	z => if (z != Double.NaN) {
	  if(abs(z) > maxDiff) { maxDiff = abs(z)}
	}
      }

      maxDiff should be <= (threshold)
    }

    describe("Hillshade") {
      ignore("calculates loaded raster correctly") {
	val threshold = 0.001 //threshold for equality

	// Expected data, from GDAL
	val gdalComputedAspectOp = io.LoadRaster("elevation-hillshade-gdal")
	server.run(gdalComputedAspectOp)
	//val elevationOp = io.LoadRaster("elevation")
	//val aspectOp = focal.Hillshade(elevationOp, Literal(315), Literal(45), Literal(1))

	//Compare the two's values
	//val diffOp = local.Subtract(aspectOp, gdalComputedAspectOp)
	//val diff = server.run(diffOp)

	// diff.foreachDouble {
	//   z => if (z != Double.NaN) {
	//     abs(z) should be <= (threshold)
	//   }
	// }
      }
    }
  }
}

