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

import org.scalatest._


import java.util.Locale

class ColorClassifierSpec extends FunSpec with Matchers {
  describe("color class construction") {
    it("should classify ints to colors") {
      val cc = new StrictIntColorClassifier
      cc.classify(123, RGBA(123))
        .classify(1234, RGBA(1234))
        .classify(1235, RGBA(1235))
        .classify(1236, RGBA(1236))
        .setNoDataColor(RGBA(8675309))
      cc.getColors shouldBe (Array(RGBA(123), RGBA(1234), RGBA(1235), RGBA(1236)))
      cc.getBreaks shouldBe (Array(123, 1234, 1235, 1236))
      cc.getNoDataColor shouldBe (RGBA(8675309))
    }

    it("should classify doubles to colors") {
      val cc = new StrictDoubleColorClassifier
      cc.classify(123.23, RGBA(123))
        .classify(12234.89, RGBA(1234))
        .classify(45.342, RGBA(1235))
        .classify(1236.13, RGBA(1236))
        .setNoDataColor(RGBA(8675309))
      cc.getColors shouldBe (Array(RGBA(1235), RGBA(123), RGBA(1236), RGBA(1234)))
      cc.getBreaks shouldBe (Array(45.342, 123.23, 1236.13, 12234.89))
      cc.getNoDataColor shouldBe (RGBA(8675309))
    }
  }
  describe("color map creation") {
    it("should build a color map with fully specifiable options") {
      val cc = StrictIntColorClassifier(Exact)
      val ndColor = RGBA(0, 0, 0, 100.0)
      val fallbackColor = RGBA(255, 0, 0, 0)
      cc.setNoDataColor(ndColor).setFallbackColor(fallbackColor)
      cc.toColorMap().options shouldBe (ColorMapOptions(Exact, ndColor.int, fallbackColor.int, false))

    }
  }
}
