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

package geotrellis.store.json

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.index._
import geotrellis.store.index.zcurve.ZSpaceTimeLargeKeyIndex

import io.circe.syntax._
import cats.syntax.either._

import org.scalatest._

class KeyIndexJsonFormatFactorySpec extends FunSpec with Matchers {
  describe("KeyIndexJsonFormatFactory"){
    it("should be able to serialize and deserialize a custom key index set through application.conf") {
      val expectedKeyBounds = KeyBounds(SpatialKey(1, 2), SpatialKey(5, 6))
      val testKeyIndex: KeyIndex[SpatialKey] = new TestKeyIndex(expectedKeyBounds)
      val json = testKeyIndex.asJson
      val actual = json.as[KeyIndex[SpatialKey]].valueOr(throw _)
      actual.keyBounds should be (expectedKeyBounds)
      actual should be (a[TestKeyIndex])
    }

    // https://github.com/geotrellis/geotrellis-server/issues/248
    it("should be able to serialize and deserialize a ZSpaceTimeLargeKeyIndex key index set through application.conf") {
      // dates range
      // ["1960-01-01T12:00Z","1974-01-01T12:00Z"]
      // unix time is in [1 January 1970; 1 January 2038) bounds
      // -315576000000L
      val minKey = SpaceTimeKey(4577, 5960, instant = -315576000000L)
      // 126273600000L
      val maxKey = SpaceTimeKey(4590, 5971, instant = 126273600000L)
      val kb = KeyBounds(minKey, maxKey)

      // val temporalResolution: Long = 31536000000L
      val index = ZSpaceTimeLargeKeyIndex.byYear(kb)
      val json = index.asJson
      val actual = json.as[KeyIndex[SpaceTimeKey]].valueOr(throw _)
      actual.keyBounds should be (kb)
      actual should be (a[ZSpaceTimeLargeKeyIndex])

      val actualt = actual.asInstanceOf[ZSpaceTimeLargeKeyIndex]
      actualt.temporalResolution shouldBe index.temporalResolution
      actualt.shift shouldBe index.shift
    }
  }
}
