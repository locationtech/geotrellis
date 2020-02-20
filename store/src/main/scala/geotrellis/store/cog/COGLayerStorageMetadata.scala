/*
 * Copyright 2018 Azavea
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

package geotrellis.store.cog

import geotrellis.store.index._
import geotrellis.layer._

import io.circe._
import io.circe.syntax._
import cats.syntax.either._

import scala.reflect._

case class COGLayerStorageMetadata[K](metadata: COGLayerMetadata[K], keyIndexes: Map[ZoomRange, KeyIndex[K]]) {
  def combine(other: COGLayerStorageMetadata[K])(implicit ev: Boundable[K]): COGLayerStorageMetadata[K] =
    COGLayerStorageMetadata(metadata.combine(other.metadata), other.keyIndexes)
}

object COGLayerStorageMetadata {
  implicit def cogLayerStorageMetadataEncoder[K: SpatialComponent: Encoder: ClassTag]: Encoder[COGLayerStorageMetadata[K]] =
    Encoder.encodeJson.contramap[COGLayerStorageMetadata[K]] { obj =>
      Json.obj(
        "metadata" -> obj.metadata.asJson,
        "keyIndexes" -> obj.keyIndexes.toVector.asJson
      )
    }
  implicit def cogLayerStorageMetadataDecoder[K: SpatialComponent: Decoder: ClassTag]: Decoder[COGLayerStorageMetadata[K]] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("metadata").as[COGLayerMetadata[K]],
        c.downField("keyIndexes").as[Vector[(ZoomRange, KeyIndex[K])]].map(_.toMap)) match {
        case (Right(md), Right(ki)) => Right(COGLayerStorageMetadata(md, ki))
        case _ => Left("COGLayerStorageMetadata expected.")
      }
    }
}
