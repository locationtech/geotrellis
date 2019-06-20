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

import spray.json.DefaultJsonProtocol._
import spray.json._

import cats.Semigroup
import cats.syntax.semigroup._

import scala.reflect._

case class COGLayerStorageMetadata[K](metadata: COGLayerMetadata[K], keyIndexes: Map[ZoomRange, KeyIndex[K]])

object COGLayerStorageMetadata {

  implicit def cogLayerStorageMetadataSemigroup[K: Boundable](implicit sg: Semigroup[COGLayerMetadata[K]]): Semigroup[COGLayerStorageMetadata[K]] =
    new Semigroup[COGLayerStorageMetadata[K]] {
      def combine(x: COGLayerStorageMetadata[K], y: COGLayerStorageMetadata[K]): COGLayerStorageMetadata[K] = {
        COGLayerStorageMetadata(x.metadata.combine(y.metadata), y.keyIndexes)
      }
    }

  implicit def cogLayerStorageMetadataFormat[K: SpatialComponent: JsonFormat: ClassTag] =
    new RootJsonFormat[COGLayerStorageMetadata[K]] {
      def write(sm: COGLayerStorageMetadata[K]) =
        JsObject(
          "metadata" -> sm.metadata.toJson,
          "keyIndexes" -> JsArray(sm.keyIndexes.map(_.toJson).toVector)
        )

      def read(value: JsValue): COGLayerStorageMetadata[K] =
        value.asJsObject.getFields("metadata", "keyIndexes") match {
          case Seq(metadata, JsArray(keyIndexes)) =>
            COGLayerStorageMetadata(
              metadata.convertTo[COGLayerMetadata[K]],
              keyIndexes.map(_.convertTo[(ZoomRange, KeyIndex[K])]).toMap
            )
          case v =>
            throw new DeserializationException(s"COGLayerStorageMetadata expected, got: $v")
        }
    }
}
