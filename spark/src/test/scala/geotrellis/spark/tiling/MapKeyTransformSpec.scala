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

package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest._

class MapKeyTransformSpec extends FunSpec with Matchers {
  describe("MapKeyTransform") {
    it("converts a grid bounds that is on the borders of the tile layout correctly") {
      val crs = LatLng
      val tileLayout = TileLayout(8, 8, 3, 4)
      val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
      val gridBounds = GridBounds(1, 1, 6, 7)
      val extent = mapTransform(gridBounds)
      val result = mapTransform(extent)

      result should be (gridBounds)
    }

    it("should correctly give grid bounds for an extent that is exactly one layout tile") {
      val ld = LayoutDefinition(Extent(630000.0, 215000.0, 645000.0, 228500.0),TileLayout(5,5,100,100))
      val e = Extent(630000.0, 215000.0, 633000.0, 217700.0)
      val mapTransform = ld.mapTransform
      val gb = mapTransform(e)

      gb should be (GridBounds(0, 4, 0, 4))
    }

    it("should give the gridbounds of the entire layout if given the extent of that layout") {
      val ld =
        LayoutDefinition(
          Extent(-31.456975828130908, 16.80232947236449, 53.8711521718691, 80.7984254723645),
          TileLayout(4,3,256,256)
        )

      val gb = ld.mapTransform(ld.extent)
      gb should be (GridBounds(0, 0, 3, 2))
    }

    it("should resepect border conditions for single tile") {
      val mp = MapKeyTransform(Extent(0.0, 0.0, 1.0, 1.0), 1, 1)
      assert(mp(Extent(0,0,1,1)) === GridBounds(0, 0, 0, 0))
    }

    it("should produce the tile south of an extent that is a point on the tile layout border") {
      val e = Extent(33.0, 50.0, 33.0, 50.0)
      val mapTransform = MapKeyTransform(Extent(0.0, 0.0, 128.0, 128.0), 64, 64)
      val gb = mapTransform(e)

      gb should be (GridBounds(16,39,16,39))
    }

    it("should return grid bounds representing the south tile for a horizontal line extent along the border of the tile layout") {
      val mp = MapKeyTransform(Extent(0.0, 0.0, 1.0, 1.0), 1, 1)
      assert(mp(Extent(0,0,1,0)) === GridBounds(0, 1, 0, 1))
    }

    it("should resepect various border conditions") {
      val mp = MapKeyTransform(Extent(0.0, 0.0, 2.0, 2.0), 2, 2)
      assert(mp(Extent(0,0,1,1)) === GridBounds(0, 1, 0, 1))
      assert(mp(Extent(1,0,2,1)) === GridBounds(1, 1, 1, 1))
      assert(mp(Extent(1,1,2,2)) === GridBounds(1, 0, 1, 0))
      assert(mp(Extent(0,1,1,2)) === GridBounds(0, 0, 0, 0))

      assert(mp(Extent(0,0,0,0)) === GridBounds(0, 2, 0, 2))
      assert(mp(Extent(0,0,2,0)) === GridBounds(0, 2, 1, 2))
    }

    it("should properly handle points north or west of the extent bounds") {
      val mp = MapKeyTransform(Extent(0.0, 0.0, 1.0, 1.0), 2, 2)
      assert(mp(Point(0.0, 1.0)) == SpatialKey(0, 0))
      assert(mp(Point(-0.1, 1.0)) == SpatialKey(-1, 0))
      assert(mp(Point(0.0, 1.1)) == SpatialKey(0, -1))
    }
  }
}
