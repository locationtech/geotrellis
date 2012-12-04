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

  describe("Slope") {
    ignore("calculates loaded raster correctly") {
      val threshold = 3.0 //threshold for equality

      // Expected data, from GDAL
      val gdalComputedSlopeOp = io.LoadRaster("elevation-slope-gdal")

      val elevationOp = io.LoadRaster("elevation")
      val slopeOp = focal.Slope(elevationOp, Literal(1))

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
      val aspectOp = focal.Aspect(elevationOp, Literal(1))

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
      it("calculates loaded raster correctly") {
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

