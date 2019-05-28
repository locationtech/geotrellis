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

package geotrellis.tiling.json

import geotrellis.tiling.{SpatialKey, SpaceTimeKey, KeyBounds, TemporalKey}

import spray.json._
import spray.json.DefaultJsonProtocol._

object KeyFormats extends KeyFormats

trait KeyFormats extends Serializable {
  implicit object SpatialKeyFormat extends RootJsonFormat[SpatialKey] {
    def write(key: SpatialKey) =
      JsObject(
        "col" -> JsNumber(key.col),
        "row" -> JsNumber(key.row)
      )

    def read(value: JsValue): SpatialKey =
      value.asJsObject.getFields("col", "row") match {
        case Seq(JsNumber(col), JsNumber(row)) =>
          SpatialKey(col.toInt, row.toInt)
        case _ =>
          throw new DeserializationException("SpatialKey expected")
      }
  }

  implicit object SpaceTimeKeyFormat extends RootJsonFormat[SpaceTimeKey] {
    def write(key: SpaceTimeKey) =
      JsObject(
        "col" -> JsNumber(key.col),
        "row" -> JsNumber(key.row),
        "instant" -> JsNumber(key.instant)
      )

    def read(value: JsValue): SpaceTimeKey =
      value.asJsObject.getFields("col", "row", "instant") match {
        case Seq(JsNumber(col), JsNumber(row), JsNumber(time)) =>
          SpaceTimeKey(col.toInt, row.toInt, time.toLong)
        case _ =>
          throw new DeserializationException("SpaceTimeKey expected")
      }
  }


  implicit object TemporalKeyFormat extends RootJsonFormat[TemporalKey] {
    def write(key: TemporalKey) =
      JsObject(
        "instant" -> JsNumber(key.instant)
      )

    def read(value: JsValue): TemporalKey =
      value.asJsObject.getFields("instant") match {
        case Seq(JsNumber(time)) =>
          TemporalKey(time.toLong)
        case _ =>
          throw new DeserializationException("TemporalKey expected")
      }
  }

  implicit def keyBoundsFormat[K: JsonFormat]: RootJsonFormat[KeyBounds[K]] =
    new RootJsonFormat[KeyBounds[K]] {
      def write(keyBounds: KeyBounds[K]) =
        JsObject(
          "minKey" -> keyBounds.minKey.toJson,
          "maxKey" -> keyBounds.maxKey.toJson
        )

      def read(value: JsValue): KeyBounds[K] =
        value.asJsObject.getFields("minKey", "maxKey") match {
          case Seq(minKey, maxKey) =>
            KeyBounds(minKey.convertTo[K], maxKey.convertTo[K])
          case _ =>
            throw new DeserializationException("${classOf[KeyBounds[K]] expected")
        }
    }
}
