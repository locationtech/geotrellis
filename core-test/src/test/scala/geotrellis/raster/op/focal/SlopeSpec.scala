package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.source._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.raster.op.transform._

import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class SlopeSpec extends FunSpec with ShouldMatchers
                                with TestServer {
  describe("Slope") {
    it("should match gdal computed slope raster") {
      val rOp = getRaster("elevation")
      val gdalOp = getRaster("slope")
      val slopeComputed = Slope(rOp,1.0)

      val rg = get(gdalOp)

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

    it("should get the same result for split raster") {
      val rOp = getRaster("elevation")
      val nonTiledSlope = Slope(rOp,1.0)

      val tiled = 
        rOp.map { r =>
          val (tcols,trows) = (11,20)
          val pcols = r.rasterExtent.cols / tcols
          val prows = r.rasterExtent.rows / trows
          val tl = TileLayout(tcols,trows,pcols,prows)
          TileRaster.wrap(r,tl)
        }

      val rs = RasterSource(tiled)
      run(rs.focalSlope) match {
        case Complete(result,success) =>
//          println(success)
          assertEqual(result,nonTiledSlope)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
