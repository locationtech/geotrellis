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

import io.circe.syntax._
import cats.syntax.either._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class KeyIndexJsonFormatFactorySpec extends AnyFunSpec with Matchers {
  describe("KeyIndexJsonFormatFactory"){
    it("should be able to serialize and deserialize a custom key index set through application.conf") {
      val expectedKeyBounds = KeyBounds(SpatialKey(1, 2), SpatialKey(5, 6))
      val testKeyIndex: KeyIndex[SpatialKey] = new TestKeyIndex(expectedKeyBounds)
      val json = testKeyIndex.asJson
      val actual = json.as[KeyIndex[SpatialKey]].valueOr(throw _)
      actual.keyBounds should be (expectedKeyBounds)
      actual should be (a[TestKeyIndex])
    }
  }
}
