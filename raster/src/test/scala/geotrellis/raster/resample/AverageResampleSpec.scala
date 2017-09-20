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

class AverageResampleSpec extends FunSpec with Matchers {

  describe("it should correctly resample to the average of a region") {

    it("should, for an integer tile, compute an average of 1 to 100 as 50") {
      val tile = IntArrayTile(1 to 100 toArray, 10, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 1, 1)
      tile.resample(extent, 1, 1, Average).get(0, 0) should be (50)
    }

    it("should, for a double tile, compute an average of .1 to 10 as roughly 5") {
      val tile = DoubleArrayTile(.1 to 10.0 by .1 toArray, 10, 10)
      val extent = Extent(0, 0, 10, 10)
      val cellsize = CellSize(extent, 1, 1)
      tile.resample(extent, 1, 1, Average).getDouble(0, 0) should be (5.0 +- 0.1)
    }
  }
}
