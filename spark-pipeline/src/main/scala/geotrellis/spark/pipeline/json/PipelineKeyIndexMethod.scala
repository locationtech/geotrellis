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

package geotrellis.spark.pipeline.json

import geotrellis.store.index.{HilbertKeyIndexMethod, KeyIndexMethod, RowMajorKeyIndexMethod, ZCurveKeyIndexMethod}
import io.circe.{Decoder, Encoder}
import geotrellis.spark.pipeline.json.CodecUtils.orDefault

case class PipelineKeyIndexMethod(
  `type`: String,
  timeTag: Option[String] = None,
  timeFormat: Option[String] = None,
  temporalResolution: Option[Int] = None
) {
  def getKeyIndexMethod[K] = (((`type`, temporalResolution) match {
    case ("rowmajor", None)    => RowMajorKeyIndexMethod
    case ("hilbert", None)     => HilbertKeyIndexMethod
    case ("hilbert", Some(tr)) => HilbertKeyIndexMethod(tr.toInt)
    case ("zorder", None)      => ZCurveKeyIndexMethod
    case ("zorder", Some(tr))  => ZCurveKeyIndexMethod.byMilliseconds(tr)
    case _                     => throw new Exception("unsupported keyIndexMethod definition")
  }): KeyIndexMethod[?]).asInstanceOf[KeyIndexMethod[K]]
}

object PipelineKeyIndexMethod {
  implicit val pipelineKeyIndexMethodEncoder: Encoder[PipelineKeyIndexMethod] =
    Encoder.forProduct4("type", "time_tag", "time_format", "temporal_resolution") { m =>
      (m.`type`, m.timeTag, m.timeFormat, m.temporalResolution)
    }

  implicit val pipelineKeyIndexMethodDecoder: Decoder[PipelineKeyIndexMethod] = Decoder.instance { c =>
    for {
      tpe <- c.get[String]("type")
      tt  <- orDefault[Option[String]](c, "time_tag", None)
      tf  <- orDefault[Option[String]](c, "time_format", None)
      tr  <- orDefault[Option[Int]](c, "temporal_resolution", None)
    } yield PipelineKeyIndexMethod(tpe, tt, tf, tr)
  }
}
