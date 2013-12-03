package geotrellis.source

import geotrellis._
import geotrellis.testutil._
import geotrellis.process._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.statistics._

class RasterSourceSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("RasterSource") {
    it("should load a tiled raster with a target extent") {
      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,_,_) =
        RasterSource("mtsthelens_tiled")
          .info
          .map(_.rasterExtent)
          .get

      val newRe = 
        RasterExtent(
          Extent(xmin,ymax-(ch*256),xmin+(cw*256),ymax),
          cw,ch,256,256)

      val uncropped = RasterSource("mtsthelens_tiled").get
      val cropped = RasterSource("mtsthelens_tiled",newRe).get

      for(row <- 0 until 256) {
        for(col <- 0 until 256) {
          withClue(s"Failed at ($col,$row)") {
            uncropped.get(col,row) should be (cropped.get(col,row))
          }
        }
      }
    }
  }
}
