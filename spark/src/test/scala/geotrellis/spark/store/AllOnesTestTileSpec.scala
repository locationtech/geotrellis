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

package geotrellis.spark.store

import geotrellis.tiling._
import geotrellis.vector.Extent
import geotrellis.raster.{GridBounds, Tile}
import geotrellis.layers._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.spark.testkit.io._

trait AllOnesTestTileSpec { self: PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] =>

  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)

  for(PersistenceSpecDefinition(keyIndexMethodName, _, layerIds) <- specLayerIds) {
    val layerId = layerIds.layerId
    val query = reader.query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)

    describe(s"AllOnes query tests for  $keyIndexMethodName") {
      it("filters past layout bounds") {
        query.where(Intersects(GridBounds(6, 2, 7, 3))).result.keys.collect() should
        contain theSameElementsAs Array(SpatialKey(6, 3), SpatialKey(6, 2))
      }

      it("query inside layer bounds") {
        val actual = query.where(Intersects(bounds1)).result.keys.collect()
        val expected = for ((x, y) <- bounds1.coordsIter.toSeq) yield SpatialKey(x, y)

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("query outside of layer bounds") {
        query.where(Intersects(GridBounds(10, 10, 15, 15))).result.collect() should be(empty)
      }

      it("disjoint query on space") {
        val actual = query.where(Intersects(bounds1) or Intersects(bounds2)).result.keys.collect()
        val expected = for ((x, y) <- bounds1.coordsIter.toSeq ++ bounds2.coordsIter.toSeq) yield SpatialKey(x, y)

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("should filter by extent") {
        val extent = Extent(-10, -10, 10, 10) // this should intersect the four central tiles in 8x8 layout
        query.where(Intersects(extent)).result.keys.collect() should
        contain theSameElementsAs {
          for ((col, row) <- GridBounds(3, 3, 4, 4).coordsIter.toSeq) yield SpatialKey(col, row)
        }
      }

      it("layer should have a proper extent after query") {
        val extent = Extent(-10, -10, 10, 10) // this should intersect the four central tiles in 8x8 layout
        val metadata = query.where(Intersects(extent)).result.metadata
        metadata.extent should be (metadata.mapTransform(GridBounds(3, 3, 4, 4)))
      }
    }
  }
}
