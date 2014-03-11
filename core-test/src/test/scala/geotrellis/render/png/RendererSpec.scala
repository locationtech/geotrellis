/***
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package geotrellis.render.png

import geotrellis.testkit._
import geotrellis.statistics.{ArrayHistogram,FastMapHistogram}
import geotrellis._
import geotrellis.render._

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
      val colorMap = renderer.colorMap.asInstanceOf[IntColorMap]

      val color:Indexed = 
        renderer.colorType match {
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
      val colorMap = renderer.colorMap.asInstanceOf[IntColorMap]
      val color:Indexed = 
        renderer.colorType match {
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
