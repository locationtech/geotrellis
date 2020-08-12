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
import geotrellis.store.index._

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

class TestKeyIndex(val keyBounds: KeyBounds[SpatialKey]) extends KeyIndex[SpatialKey] {
  def toIndex(key: SpatialKey): BigInt = BigInt(1)

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] =
    Seq((BigInt(1), BigInt(2)))
}

class TestKeyIndexRegistrator extends KeyIndexRegistrator {
  val test = "test"

  implicit val testKeyIndexEncoder: Encoder[TestKeyIndex] =
    Encoder.encodeJson.contramap[TestKeyIndex] { obj =>
      Json.obj(
        "type"   -> test.asJson,
        "properties" -> Json.obj("keyBounds" -> obj.keyBounds.asJson)
      )
    }

  implicit val testKeyIndexDecoder: Decoder[TestKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != test) Left(s"Wrong KeyIndex type: $test expected.")
          else
            properties
              .downField("keyBounds")
              .as[KeyBounds[SpatialKey]]
              .map(new TestKeyIndex(_))
              .leftMap(_ => "Couldn't deserialize test key index.")

        case _ => Left("Wrong KeyIndex type: test key index expected.")
      }
    }

  def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
    keyIndexRegistry register KeyIndexFormatEntry[SpatialKey, TestKeyIndex](test)
  }
}
