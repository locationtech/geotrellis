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

package geotrellis.spark.io.json

import io.circe._
import io.circe.generic.semiauto._

import geotrellis.spark._

object KeyFormats extends KeyFormats

trait KeyFormats extends Serializable {
  implicit val spatialKeyEncoder: Encoder[SpatialKey] = deriveEncoder
  implicit val spatialKeyDecoder: Decoder[SpatialKey] = deriveDecoder

  implicit val spaceTimeKeyEncoder: Encoder[SpaceTimeKey] = deriveEncoder
  implicit val spaceTimeDecoder: Decoder[SpaceTimeKey] = deriveDecoder

  implicit val temporalKeyEncoder: Encoder[TemporalKey] = deriveEncoder
  implicit val temporalKeyDecoder: Decoder[TemporalKey] = deriveDecoder

  implicit def keyBoundsEncoder[K: Encoder]: Encoder[KeyBounds[K]] = deriveEncoder
  implicit def keyBoundsDecoder[K: Decoder]: Decoder[KeyBounds[K]] = deriveDecoder
}
