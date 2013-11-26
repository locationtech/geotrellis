package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.raster._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner 
import org.junit.Assert._

import geotrellis.testutil._

class FillSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {

  describe("Fill"){
    it("Returns a new raster with sinks removed"){
      var ncols = 6
      var nrows = 6
      val re = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val m = IntArrayRasterData(Array[Int](
            2,2,2,4,4,8,
            1,1,1,4,8,4,
            1,128,1,2,4,8,
            1,1,1,4,4,4,
            1,2,3,4,5,6,
            1,1,1,1,4,16),
            ncols,nrows)

      val inRaster = Raster(m, re)
      val o = IntArrayRasterData(Array[Int](
            2,2,2,4,4,8,
            1,1,1,4,8,4,
            1,1,1,2,4,8,
            1,1,1,4,4,4,
            1,2,3,4,5,6,
            1,1,1,1,4,16),
            ncols,nrows)
      val outRaster = Raster(o, re)
      assertEqual(Fill(inRaster),outRaster )
    } 
  }
}
