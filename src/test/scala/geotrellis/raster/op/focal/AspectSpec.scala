package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.raster.op.transform._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner
import scala.math._

import Angles._

@RunWith(classOf[JUnitRunner])
class AspectSpec extends FunSpec with ShouldMatchers
                                 with TestServer 
                                 with RasterBuilders {
  describe("Aspect") {
    it("should match gdal computed aspect raster") {
      val rOp = get("elevation")
      val gdalOp = get("aspect")
      val aspectComputed = Aspect(rOp)

      val rg = run(gdalOp)
      var re = run(aspectComputed)

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results
      val (xmin,ymax) = rg.rasterExtent.gridToMap(1,1)
      val (xmax,ymin) = rg.rasterExtent.gridToMap(rg.cols-2, rg.rows-2)

      val cropExtent = Extent(xmin,ymin,xmax,ymax)
      val croppedGdal = Crop(gdalOp,cropExtent)
      val croppedComputed = Crop(aspectComputed,cropExtent)

      val rgc = run(Force(croppedGdal))
      val rc = run(Force(croppedComputed))

      assertEqual(croppedGdal,croppedComputed, 0.1)
    }

    it("should work with tiling") {
      val rOp = get("elevation")
      val nonTiledAspect = Aspect(rOp)

      val tiled = logic.Do(rOp)({ r => Tiler.createTiledRaster(r,89,140) })
      val tiledAspect = TileFocalOp(tiled,Aspect(_))

      assertEqual(nonTiledAspect,tiledAspect)
    }

    it("should calculate edge cases correctly") {
      val r = createRaster(Array[Int](-1,0,1,1,1,
                                       1,2,2,2,2,
                                       1,2,2,2,2,
                                       1,2,2,2,2,
                                       1,2,2,1,2))

      val aR = run(Aspect(r))

      // Check left edge
      var value = aR.getDouble(0,1)

      var dx = ((0-1) + 2*(2-1) + (2-1)) / 8.0
      var dy = ((1-1) + 2*(1-(-1)) + (2-0)) / 8.0

      var aspect = atan2(dy,-dx) / (Pi / 180.0)
      value should equal (aspect)

      //Check right edge
      value = aR.getDouble(4,1)

      dx = ((2-1) + 2*(2-2) + (2-2)) / 8.0
      dy = ((2-1) + 2*(2-1) + (2-2)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      (value-aspect) should be < 0.0000001

      //Check bottom edge
      value = aR.getDouble(1,4)

      dx = ((2-1) + 2*(2-1) + (2-2)) / 8.0
      dy = ((2-1) + 2*(2-2) + (2-2)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      (value-aspect) should be < 0.0000001

      //Check top edge
      value = aR.getDouble(3,0)

      dx = ((1-1) + 2*(1-1) + (2-2)) / 8.0
      dy = ((2-1) + 2*(2-1) + (2-1)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      (value-aspect) should be < 0.0000001

      //Check top right corner 
      value = aR.getDouble(4,0)

      dx = ((1-1) + 2*(1-1) + (1-2)) / 8.0
      dy = ((2-1) + 2*(2-1) + (1-1)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      (value-aspect) should be < 0.0000001

      //Check top left corner
      value = aR.getDouble(0,0)

      dx = (((-1)-(-1)) + 2*(0-(-1)) + (2-(-1))) / 8.0
      dy = (((-1)-(-1)) + 2*(1-(-1)) + (2-(-1))) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      (value-aspect) should be < 0.0000001

      //Check bottom left corner
      value = aR.getDouble(0,4)

      dx = ((2-1) + 2*(2-1) + (1-1)) / 8.0
      dy = ((1-1) + 2*(1-1) + (1-2)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      if(aspect < 0.0) { aspect += 360 }
      (value-aspect) should be < 0.000001

      //Check bottomr right corner
      value = aR.getDouble(4,4)

      dx = ((2-2) + 2*(2-1) + (2-2)) / 8.0
      dy = ((2-12) + 2*(2-2) + (2-2)) / 8.0

      aspect = atan2(dy,-dx) / (Pi / 180.0)
      if(aspect < 0.0) { aspect += 360 }
      (value-aspect) should be < 0.0000001
    }
  }
}
