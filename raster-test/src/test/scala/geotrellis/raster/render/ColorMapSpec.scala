/*
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
 */

package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.render.png._
import geotrellis.raster.testkit._

import org.scalatest._

class ColorMapSpec extends FunSpec with Matchers
                                   with TileBuilders {
  describe("PNG Color Mapping") {
    it("should correctly map values to colors") {
      val limits = Array(25,50,80,100)
      val colors = Array(100,110,120,130).map(RGBA(_))
      val colorClassifier = new StrictIntColorClassifier
      colorClassifier.addClassifications(limits zip colors).setNoDataColor(RGBA(0))

      val arr = (0 until 90 by 5).toArray
      val r = createTile(arr)

      val colorMap = colorClassifier.toColorMap().asInstanceOf[IntColorMap]

      val color:IndexedPngEncoding =
        PngColorEncoding(colorClassifier.getColors, colorClassifier.getNoDataColor) match {
          case i @ IndexedPngEncoding(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      for(x <- arr) {
        if(x <= 25) colorMap(x) should be (100)
        else if(x <= 50) colorMap(x) should be (110)
        else if (x <= 80) colorMap(x) should be (120)
        else { colorMap(x) should be (130) }
      }
    }

    it("should correctly map redundant values to colors") {
      val limits = Array(25,42,60)
      val colors = Array(10,20,30).map(RGBA(_))
      val colorClassifier = new StrictIntColorClassifier
      colorClassifier.addClassifications(limits zip colors).setNoDataColor(RGBA(0))

      val arr = Array(10,10,10,10,10,10,10,20,20,20,20,30,30,30,40,50)
      val r = createTile(arr)

      val colorMap = colorClassifier.toColorMap().asInstanceOf[IntColorMap]
      val color:IndexedPngEncoding =
        PngColorEncoding(colorClassifier.getColors, colorClassifier.getNoDataColor) match {
          case i @ IndexedPngEncoding(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      colorMap(10) should be (10)
      colorMap(20) should be (10)
      colorMap(30) should be (20)
      colorMap(40) should be (20)
      colorMap(50) should be (30)
    }
  }
}
