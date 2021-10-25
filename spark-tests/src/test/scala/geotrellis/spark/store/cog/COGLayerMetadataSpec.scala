/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.store.cog

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.layer._
import geotrellis.raster._
import geotrellis.store
import geotrellis.store.cog._
import geotrellis.spark._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class COGLayerMetadataSpec extends AnyFunSpec with Matchers {
  def generateLCMetadata(maxZoom: Int = 13, minZoom: Int = 0): COGLayerMetadata[SpatialKey] = {
    val cellType = IntConstantNoDataCellType
    val extent = Extent(1.5454921707580032E7, 4146985.8273180854, 1.5762771754498206E7, 4454374.804079672)
    val crs = WebMercator
    val keyBounds = KeyBounds(SpatialKey(7255,3185), SpatialKey(7318,3248))
    val layoutScheme = ZoomedLayoutScheme(WebMercator)
    val maxTileSize = 4096

    COGLayerMetadata(cellType, extent, crs, keyBounds, layoutScheme, maxZoom, minZoom, maxTileSize)
  }

  describe("COGLayerMetadata.apply") {
    it("should produce correct metadata for landsat scene example, build all partial pyramids correct") {
      val expectedZoomRanges = Vector(store.cog.ZoomRange(0,0), store.cog.ZoomRange(1, 1), store.cog.ZoomRange(2,2), store.cog.ZoomRange(3,3), store.cog.ZoomRange(4,4), store.cog.ZoomRange(5,5), store.cog.ZoomRange(6,6), store.cog.ZoomRange(7,8), store.cog.ZoomRange(9,13))

      val md = generateLCMetadata()
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }

    it("should produce correct metadata for landsat scene example, build until zoom level that is not a lower zoom level of some partial pyramid") {
      val expectedZoomRanges = Vector(store.cog.ZoomRange(7,8), store.cog.ZoomRange(9,13))

      val md = generateLCMetadata(minZoom = 8)
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }

    it("should produce correct metadata for landsat scene example, build until zoom level where this level is both min and max level of this partial pyramid") {
      val expectedZoomRanges = Vector(store.cog.ZoomRange(6,6), store.cog.ZoomRange(7,8), store.cog.ZoomRange(9,13))

      val md = generateLCMetadata(minZoom = 6)
      // println(md.toJson.prettyPrint)
      md.zoomRanges should contain theSameElementsAs (expectedZoomRanges)
    }
  }

  describe("COGLayerMetadata.zoomRangeInfoFor") {
    it("should return the requested zoomRangeInfo, regardless of whether zoomRangeInfos are sorted or not") {

      val md = generateLCMetadata()
      val mdUnsorted = md.copy(zoomRangeInfos = md.zoomRangeInfos.reverse)

      mdUnsorted.zoomRangeFor(5) should equal (ZoomRange(5,5))
    }
  }

  describe("COGLayerMetadata.combine") {
    it("should produce combined metadata with zoomRangeInfos sorted") {

      val md = generateLCMetadata()
      val mdOther = generateLCMetadata()
      val mdCombined = md.combine(mdOther)

      implicit val ordering: Ordering[(ZoomRange, KeyBounds[SpatialKey])] = Ordering.by(_._1)
      mdCombined.zoomRangeInfos should be (sorted)
    }
  }
}
