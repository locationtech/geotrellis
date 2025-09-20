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

package geotrellis.raster.io.geotiff.compression

import io.circe._
import io.circe.syntax._

trait Compression extends Serializable {
  def createCompressor(segmentCount: Int): Compressor

  def withPredictor(predictor: Predictor): Compression =
    (segmentCount: Int) => createCompressor(segmentCount).withPredictorEncoding(predictor)
}

object Compression {
  implicit val compressionDecoder: Decoder[Compression] =
    (c: HCursor) =>
      c.downField("compressionType").as[String].map {
        case "NoCompression" => NoCompression
        case _ =>
          c.downField("level").as[Int] match {
            case Left(_) => DeflateCompression()
            case Right(i) => DeflateCompression(i)
          }
      }

  implicit val compressionEncoder: Encoder[Compression] = {
    case NoCompression =>
      Json.obj(("compressionType", "NoCompression".asJson))
    case d: DeflateCompression =>
      Json.obj(("compressionType", "Deflate".asJson), ("level", d.level.asJson))
  }
}
