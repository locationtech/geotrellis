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

package geotrellis.raster

import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import org.scalatest._

class CroppedTileSpec
  extends FunSpec
    with TileBuilders
    with RasterMatchers
    with TestFiles {

  describe("CroppedTileSpec") {
    it("should combine cropped tile") {
      val r = createTile(
        Array[Int](
          1, 1, 1, 1, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 2, 2, 2, 1,
          1, 1, 1, 1, 1))

      val sourceExtent = Extent(0, 0, 5, 5)
      val targetExtent = Extent(1, 1, 4, 4)
      val tile = CroppedTile(r, sourceExtent, targetExtent).toArrayTile

      assertEqual(tile.combine(tile)(_ + _), Array[Int](
        4, 4, 4,
        4, 4, 4,
        4, 4, 4))
    }
  }
}
