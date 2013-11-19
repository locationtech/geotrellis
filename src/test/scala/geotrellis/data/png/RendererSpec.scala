package geotrellis.data.png

import geotrellis.testutil._
import geotrellis.statistics.{ArrayHistogram,FastMapHistogram}
import geotrellis._
import geotrellis.data._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class RendererSpec extends FunSpec with ShouldMatchers 
                                   with RasterBuilders {
  describe("PNG Renderer") {
    it("should work with ArrayHistogram") {
      val limits = Array(25,50,80,100)
      val colors = Array(100,110,120,130)

      val arr = (0 until 90 by 5).toArray
      val r = createRaster(arr)

      val nodata = 0
      val renderer = Renderer(limits,colors,nodata)
      val colorMap = renderer.colorMap

      val color:Indexed = 
        renderer.color match {
          case i @ Indexed(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      for(x <- arr) {
        if(x <= 25) color.as(colorMap(x)) should be (100)
        else if(x <= 50) color.as(colorMap(x)) should be (110)
        else if (x <= 80) color.as(colorMap(x)) should be (120)
        else { color.as(colorMap(x)) should be (130) }
      }
    }

    it("should map color correctly for histogram with varying values and counts") {
      val limits = Array(25,42,60)
      val colors = Array(10,20,30)

      val arr = Array(10,10,10,10,10,10,10,20,20,20,20,30,30,30,40,50)
      val r = createRaster(arr)

      val nodata = 0
      val renderer = Renderer(limits,colors,nodata)
      val colorMap = renderer.colorMap
      val color:Indexed = 
        renderer.color match {
          case i @ Indexed(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      color.as(colorMap(10)) should be (10)
      color.as(colorMap(20)) should be (10)
      color.as(colorMap(30)) should be (20)
      color.as(colorMap(40)) should be (20)
      color.as(colorMap(50)) should be (30)
    }
  }
}
