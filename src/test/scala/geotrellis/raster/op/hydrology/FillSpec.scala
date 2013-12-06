package geotrellis.raster.op.hydrology

import geotrellis._
import geotrellis.raster._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.testutil._

class FillSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Fill"){
    it("Returns a new raster with sinks removed"){
      var ncols = 3
      var nrows = 3
      val re = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val m = IntArrayRasterData(Array[Int](
            1,2,3,
            4,55,6,
            7,8,9),
            ncols,nrows)

      val inRaster = Raster(m, re)
      val o = IntArrayRasterData(Array[Int](
            1,2,3,
            4,5,6,
            7,8,9),
            ncols,nrows)
      val outRaster = Raster(o, re)
      assertEqual(Fill(inRaster),outRaster )
    } 

    it("Does not remove non-sink even past the threshold"){
      var ncols = 3
      var nrows = 3
      val re = RasterExtent(Extent(0,0,1,1),1,1,ncols,nrows)
      val m = IntArrayRasterData(Array[Int](
            1,2,100,
            4,55,130,
            80,145,132),
            ncols,nrows)

      val inRaster = Raster(m, re)
      assertEqual(Fill(inRaster,FillOptions(50)),inRaster )
    } 
  }
}
