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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class MedianResampleSpec extends FunSpec with Matchers {

  describe("it should resample to nodata when only nodata in tile") {

    it("should for a integer tile compute nodata as most common value") {
      val tile = IntArrayTile(1 to 100 toArray, 10, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 10, 10)
      val resamp = new MedianResample(tile, extent, cellsize)
      tile.resample(extent, 1, 1, Median).get(0, 0) should be (50)
    }

    it("should for a double tile compute nodata as most common value") {
      val tile = DoubleArrayTile(0.1 to 10 by 0.1 toArray, 10, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 10, 10)
      val resamp = new MedianResample(tile, extent, cellsize)
      tile.resample(extent, 1, 1, Median).getDouble(0, 0) should be (5.05)
    }

    it("should return the mean of the two median-most values for an even set of contributing cells") {
      val tile = DoubleArrayTile(Array(10, 20), 2, 1)
      val extent = Extent(0, 0, 2, 1)
      val cellsize = CellSize(extent, 2, 1)
      val resamp = new MedianResample(tile, extent, cellsize)
      tile.resample(extent, 1, 1, Median).get(0, 0) should be (15)
    }

  }
}
