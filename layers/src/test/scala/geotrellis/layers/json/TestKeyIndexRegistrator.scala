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

package geotrellis.layers.json

import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers._
import geotrellis.layers.index._

import spray.json._
import spray.json.DefaultJsonProtocol._

class TestKeyIndex(val keyBounds: KeyBounds[SpatialKey]) extends KeyIndex[SpatialKey] {
  def toIndex(key: SpatialKey): BigInt = BigInt(1)

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] =
    Seq((BigInt(1), BigInt(2)))
}

class TestKeyIndexRegistrator extends KeyIndexRegistrator {
  implicit object TestKeyIndexFormat extends RootJsonFormat[TestKeyIndex] {
    final def TYPE_NAME = "test"

    def write(obj: TestKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): TestKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new TestKeyIndex(kb.convertTo[KeyBounds[SpatialKey]])
            case _ =>
              throw new DeserializationException(
                "Couldn't deserialize test key index.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: test key index expected.")
      }
  }

  def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
    keyIndexRegistry register KeyIndexFormatEntry[SpatialKey, TestKeyIndex](TestKeyIndexFormat.TYPE_NAME)
  }
}
