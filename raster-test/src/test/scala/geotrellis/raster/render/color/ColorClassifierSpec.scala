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
      val colorMap =
        ColorMap(
          123 -> 123,
          1234 -> 1234,
          1235 -> 1235,
          1236 -> 1236
        ).withNoDataColor(8675309)

      colorMap.colors shouldBe (Array(123, 1234, 1235, 1236))
      colorMap.options.noDataColor shouldBe (8675309)
    }

    it("should classify doubles to colors") {
      val colorMap =
        ColorMap(
          123.23 -> 123,
          12234.89 -> 1234,
          45.342 -> 1235,
          1236.13 -> 1236
        ).withNoDataColor(8675309)
      colorMap.colors shouldBe (Array(1235, 123, 1236, 1234))
      colorMap.options.noDataColor shouldBe (8675309)
    }
  }

  describe("color map creation") {
    it("should build a color map with fully specifiable options") {
      val ndColor = RGBA(0, 0, 0, 100.0)
      val fallbackColor = RGBA(255, 0, 0, 0)
      val colorMap =
        ColorMap((0, 1))
          .withNoDataColor(ndColor)
          .withFallbackColor(fallbackColor)
          .withBoundaryType(Exact)

      colorMap.options shouldBe (ColorMap.Options(Exact, ndColor.int, fallbackColor.int, false))
    }
  }
}
