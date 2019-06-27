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

package geotrellis.vector.io.json

import io.circe._
import io.circe.syntax._

import scala.collection.mutable

case class Style(
  strokeColor: Option[String],
  strokeWidth: Option[String],
  strokeOpacity: Option[Double],
  fillColor: Option[String],
  fillOpacity: Option[Double]
)

object Style {
  def apply(
    strokeColor: String = "",
    strokeWidth: String = "",
    strokeOpacity: Double = Double.NaN,
    fillColor: String = "",
    fillOpacity: Double = Double.NaN
  ): Style =
    Style(
      if (strokeColor != "") Some(strokeColor) else None,
      if (strokeWidth != "") Some(strokeWidth) else None,
      if (!java.lang.Double.isNaN(strokeOpacity)) Some(strokeOpacity) else None,
      if (fillColor != "") Some(fillColor) else None,
      if (!java.lang.Double.isNaN(fillOpacity)) Some(fillOpacity) else None
    )

  implicit val styleDecoder: Decoder[Style] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      val strokeColor =
        c.downField("stroke").as[String] match {
          case Right(v) => Some(v)
          case _ => None // throw new Exception(s"'stroke' property must be a string")
        }

      val strokeWidth =
        c.downField("stroke-width").as[String] match {
          case Right(v) => Some(v)
          case _ => None // throw new Exception(s"'stroke-width' property must be a string")
        }

      val strokeOpacity =
        c.downField("stroke-opacity").as[Double] match {
          case Right(v) => Some(v)
          case _ => None // throw new Exception(s"'stroke-opacity' property must be a double")
        }

      val fillColor =
        c.downField("fill").as[String] match {
          case Right(v) => Some(v)
          case _ => None // throw new Exception(s"'fill' property must be a string")
        }

      val fillOpacity =
        c.downField("fill-opacity").as[Double] match {
          case Right(v) => Some(v)
          case _ => None // throw new Exception(s"'fill-opacity' property must be a double")
        }

      Right(Style(strokeColor, strokeWidth, strokeOpacity, fillColor, fillOpacity))
    }

  implicit val styleEncoder: Encoder[Style] =
    Encoder.encodeJson.contramap[Style] { style =>
      val l = mutable.ListBuffer[(String, Json)]()
      if (style.strokeColor.isDefined) l += (("stroke", style.strokeColor.get.asJson))
      if (style.strokeWidth.isDefined) l += (("stroke-width", style.strokeWidth.get.asJson))
      if (style.strokeOpacity.isDefined) l += (("stroke-opacity", style.strokeOpacity.get.asJson))
      if (style.fillColor.isDefined) l += (("fill", style.fillColor.get.asJson))
      if (style.fillOpacity.isDefined) l += (("fill-opacity", style.fillOpacity.get.asJson))

      Json.fromFields(l)
    }
}
