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
import geotrellis.raster.{GridBounds, Tile}
import geotrellis.layers._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles

import java.time.{ZoneOffset, ZonedDateTime}


trait CoordinateSpaceTimeSpec { self: PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] =>
  val dates = Vector( // all the dates in the layer
    ZonedDateTime.of(2010,1,1,0,0,0,0, ZoneOffset.UTC),
    ZonedDateTime.of(2011,1,1,0,0,0,0, ZoneOffset.UTC),
    ZonedDateTime.of(2012,1,1,0,0,0,0, ZoneOffset.UTC),
    ZonedDateTime.of(2013,1,1,0,0,0,0, ZoneOffset.UTC),
    ZonedDateTime.of(2014,1,1,0,0,0,0, ZoneOffset.UTC))
  val bounds1 = GridBounds(1,1,3,3)
  val bounds2 = GridBounds(4,5,6,6)

  for(PersistenceSpecDefinition(keyIndexMethodName, _, layerIds) <- specLayerIds) {
    val layerId = layerIds.layerId
    val query = reader.query[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](layerId)
    describe(s"CoordinateSpaceTime query tests for $keyIndexMethodName") {
      it("query outside of layer bounds") {
        query.where(Intersects(GridBounds(10, 10, 15, 15))).result.collect() should be(empty)
      }

      it("query disjunction on space") {
        val actual = query.where(Intersects(bounds1) or Intersects(bounds2)).result.keys.collect()

        val expected = {
          for {
            (col, row) <- bounds1.coordsIter.toSeq ++ bounds2.coordsIter.toSeq
            time <- dates
          } yield SpaceTimeKey(col, row, time)
        }

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("query disjunction on space and time") {
        val actual = query.where(Intersects(bounds1) or Intersects(bounds2))
          .where(Between(dates(0), dates(1)) or Between(dates(3), dates(4))).result.keys.collect()

        val expected = {
          for {
            (col, row) <- bounds1.coordsIter.toSeq ++ bounds2.coordsIter.toSeq
            time <- dates diff Seq(dates(2))
          } yield {
            SpaceTimeKey(col, row, time)
          }
        }

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }

      it("query at particular times") {
        val actual = query.where(Intersects(bounds1) or Intersects(bounds2))
          .where(At(dates(0)) or At(dates(4))).result.keys.collect()

        val expected = {
          for {
            (col, row) <- bounds1.coordsIter.toSeq ++ bounds2.coordsIter.toSeq
            time <- Seq(dates(0), dates(4))
          } yield {
            SpaceTimeKey(col, row, time)
          }
        }

        if (expected.diff(actual).nonEmpty)
          info(s"missing: ${(expected diff actual).toList}")
        if (actual.diff(expected).nonEmpty)
          info(s"unwanted: ${(actual diff expected).toList}")

        actual should contain theSameElementsAs expected
      }
    }
  }
}
