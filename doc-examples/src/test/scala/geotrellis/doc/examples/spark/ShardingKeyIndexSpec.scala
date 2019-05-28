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

package geotrellis.doc.examples.spark

import geotrellis.tiling.{SpatialKey, SpaceTimeKey, KeyBounds}
import geotrellis.spark._
import geotrellis.layers.io.index._

import org.scalatest._
import spray.json._
import spray.json.DefaultJsonProtocol._

// --- //

class ShardingKeyIndexSpec extends FunSpec with Matchers {
  /* Z-Curve Indices */
  val zspace: KeyIndex[SpatialKey] =
    ZCurveKeyIndexMethod.createIndex(KeyBounds(
      SpatialKey(0,0),
      SpatialKey(9,9)
    ))

  val zspaceTime: KeyIndex[SpaceTimeKey] =
    ZCurveKeyIndexMethod.byDay.createIndex(KeyBounds(
      SpaceTimeKey(0, 0, 1),
      SpaceTimeKey(9, 9, 10)
    ))

  /* Hilbert Indices */
  val hspace: KeyIndex[SpatialKey] =
    HilbertKeyIndexMethod.createIndex(KeyBounds(
      SpatialKey(0, 0), SpatialKey(9, 9))
    )

  val hspaceTime: KeyIndex[SpaceTimeKey] =
    HilbertKeyIndexMethod(10).createIndex(KeyBounds(
      SpaceTimeKey(0, 0, 1), SpaceTimeKey(10, 10, 100))
    )

  describe("Index creation") {
    it("object construction") {
      new ShardingKeyIndex(zspace, 5)
      new ShardingKeyIndex(zspaceTime, 5)
      new ShardingKeyIndex(hspace, 5)
      new ShardingKeyIndex(hspaceTime, 5)
    }
  }

  describe("JsonFormat") {
    it("Z-Space Isomorphism") {
      val index: KeyIndex[SpatialKey] = new ShardingKeyIndex(zspace, 5)

      index.toJson.convertTo[KeyIndex[SpatialKey]]
    }

    it("Z-Time Isomorphism") {
      val index: KeyIndex[SpaceTimeKey] = new ShardingKeyIndex(zspaceTime, 5)

      index.toJson.convertTo[KeyIndex[SpaceTimeKey]]
    }

    it("H-Space Isomorphism") {
      val index: KeyIndex[SpatialKey] = new ShardingKeyIndex(hspace, 5)

      index.toJson.convertTo[KeyIndex[SpatialKey]]
    }

    it("H-Time Isomorphism") {
      val index: KeyIndex[SpaceTimeKey] = new ShardingKeyIndex(hspaceTime, 5)

      index.toJson.convertTo[KeyIndex[SpaceTimeKey]]
    }

  }
}
