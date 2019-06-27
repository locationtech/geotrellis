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

package geotrellis.store

import io.circe._
import cats.syntax.either._

trait LayerType {
  lazy val name = this.getClass.getName.split("\\$").last.split("\\.").last
  override def toString = name
}

object LayerType {
  def fromString(str: String): LayerType =
    str match {
      case AvroLayerType.name => AvroLayerType
      case COGLayerType.name => COGLayerType
      case _ => throw new Exception(s"Could not derive LayerType from given string: $str")
    }

  implicit val layerTypeEncoder: Encoder[LayerType] = Encoder.encodeString.contramap[LayerType] { _.toString }
  implicit val layerTypeDecoder: Decoder[LayerType] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(LayerType.fromString(str)).leftMap(_ => s"LayerType expected, got $str")
  }
}

case object AvroLayerType extends LayerType
case object COGLayerType extends LayerType
