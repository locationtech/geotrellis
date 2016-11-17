/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.matching

import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram

import org.scalatest._


class HistogramMatchingSpec extends FunSpec with Matchers
{
  describe("Histogram Matching") {

    val sourceHistogram = {
      val h = StreamingHistogram(42)
      h.countItem(1, 1)
      h.countItem(2, 2)
      h.countItem(4, 3)
      h.countItem(8, 4)
      h.countItem(16, 5)
      h
    }

    val targetHistogram = {
      val h = StreamingHistogram(42)
      h.countItem(1, 1)
      h.countItem(2, 2)
      h.countItem(3, 3)
      h.countItem(4, 4)
      h.countItem(5, 5)
      h
    }

    val sourceHistograms = List(sourceHistogram, sourceHistogram)
    val targetHistograms = List(targetHistogram, targetHistogram)

    it("should work on floating-point rasters") {
      val tile = DoubleArrayTile(Array[Double](16, 1, 2, 4, 8, 16), 2, 3)
      val actual = tile.matchHistogram(sourceHistogram, targetHistogram)
      val expected = DoubleArrayTile(Array[Double](5, 1, 2, 3, 4, 5), 2, 3)

      actual.toArray.toList should be (expected.toArray.toList)
    }

    it("should work on unsigned integral rasters") {
      val tile = UShortArrayTile(Array[Short](16, 1, 2, 4, 8, 16), 2, 3)
      val actual = tile.matchHistogram(sourceHistogram, targetHistogram)
      val expected = UShortArrayTile(Array[Short](5, 1, 2, 3, 4, 5), 2, 3)

      actual.toArray.toList should be (expected.toArray.toList)
    }

    it("should work on signed integral rasters") {
      val tile = ShortArrayTile(Array[Short](16, 1, 2, 4, 8, 16), 2, 3)
      val actual = tile.matchHistogram(sourceHistogram, targetHistogram)
      val expected = ShortArrayTile(Array[Short](5, 1, 2, 3, 4, 5), 2, 3)

      actual.toArray.toList should be (expected.toArray.toList)
    }

    it("should work on multiband tiles") {
      val band1 = ShortArrayTile(Array[Short](16, 1, 2, 4, 8, 16), 2, 3)
      val band2 = ShortArrayTile(Array[Short](4, 8, 16, 16, 1, 2), 2, 3)
      val tile = ArrayMultibandTile(band1, band2)
      val actual = tile.matchHistogram(sourceHistograms, targetHistograms).bands.flatMap({ _.toArray.toList })
      val expected = List(5, 1, 2, 3, 4, 5, 3, 4, 5, 5, 1, 2)

      actual should be (expected)
    }

  }
}
