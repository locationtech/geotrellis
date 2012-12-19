package geotrellis.data.png

import geotrellis.testutil._
import geotrellis.statistics.{ArrayHistogram,FastMapHistogram}
import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class RendererSpec extends FunSpec with ShouldMatchers 
                                   with RasterBuilders {
  describe("PNG Renderer") {
    it("should work with ArrayHistogram") {
      val limits = Array(25,50,80)
      val colors = Array(100,110,120)

      val arr = (0 until 90 by 5).toArray
      val r = createRaster(arr)
      val h = ArrayHistogram.fromRaster(r,arr.max+1)

      val nodata = 0
      val renderer = Renderer(limits,colors,h,nodata,TypeInt,Rgba)

      for(x <- arr) {
        if(x <= 25) renderer(x) should be (100)
        else if(x <= 50) renderer(x) should be (110)
        else if (x <= 80) renderer(x) should be (120)
        else renderer(x) should be (120)
      }
    }

    it("should map color correctly for histogram with varying values and counts") {
      val limits = Array(25,42,60)
      val colors = Array(10,20,30)

      val arr = Array(10,10,10,10,10,10,10,20,20,20,20,30,30,30,40,50)
      val r = createRaster(arr)
      val h = FastMapHistogram.fromRaster(r)

      val nodata = 0
      val renderer = Renderer(limits,colors,h,nodata,TypeInt,Rgba)
    
      renderer(10) should be (10)
      renderer(20) should be (10)
      renderer(30) should be (20)
      renderer(40) should be (20)
      renderer(50) should be (30)
    }
  }
}
