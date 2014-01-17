package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import scala.math.abs

import geotrellis._
import geotrellis.source._
import geotrellis.data.arg._
import geotrellis.testutil._
import geotrellis.raster._
import geotrellis.data._

class TileSetRasterLayerSpec extends FunSpec 
                            with MustMatchers 
                            with ShouldMatchers 
                            with TestServer 
                            with RasterBuilders {
  describe("A TileSetRasterLayer") {
    it("should get a cropped version correctly") {
      val re = RasterSource("albers_DevelopedLand").rasterExtent.get
//      println(RasterSource("albers_DevelopedLand").info.get.rasterType)//.rasterExtent.get
      val Extent(xmin, ymin, xmax, ymax) = re.extent
      val newRe = 
        RasterExtent(Extent(xmin,ymin,(xmin+xmax)/2.0,(ymin+ymax)/2.0),
                     re.cellwidth,
                     re.cellheight,
                     re.cols/2,
                     re.rows/2)

      val rs = RasterSource("albers_DevelopedLand", newRe)

      rs.get.rasterExtent should be (newRe)
    }
  }

}
