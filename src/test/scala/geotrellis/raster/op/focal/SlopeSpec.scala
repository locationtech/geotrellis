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

@RunWith(classOf[JUnitRunner])
class SlopeSpec extends FunSpec with ShouldMatchers
                                with TestServer {
  describe("Slope") {
    it("should match gdal computed slope raster") {
      val rOp = get("elevation")
      val gdalOp = get("slope")
      val slopeComputed = Slope(rOp,1.0)

      val rg = run(gdalOp)

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results, but don't throw the baby out
      // with the bathwater. The inner results should match.
      val (xmin,ymax) = rg.rasterExtent.gridToMap(1,1)
      val (xmax,ymin) = rg.rasterExtent.gridToMap(rg.cols - 2, rg.rows - 2)

      val cropExtent = Extent(xmin,ymin,xmax,ymax)
      val croppedGdal = Crop(gdalOp,cropExtent)
      val croppedComputed = Crop(slopeComputed,cropExtent)
      
      assertEqual(croppedGdal,croppedComputed, 1.0)
    }

    it("should work with tiling") {
      val rOp = get("elevation")
      val nonTiledSlope = Slope(rOp,1.0)

      val tiled = logic.Do(rOp)({ r => Tiler.createTiledRaster(r,89,140) })
      //val tiledSlope = TileFocalOp(tiled,Slope(rOp,1.0))
      val tiledSlope = Slope(tiled, 1.0)
      assertEqual(nonTiledSlope,tiledSlope)
    }
  }
}
