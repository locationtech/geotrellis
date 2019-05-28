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

package geotrellis.layers.cog

import spray.json._

case class ZoomRange(minZoom: Int, maxZoom: Int) {
  def isSingleZoom: Boolean = minZoom == maxZoom

  def slug: String = s"${minZoom}_${maxZoom}"

  def zoomInRange(zoom: Int): Boolean =
    zoom >= minZoom && zoom <= maxZoom
}

object ZoomRange {
  implicit def ordering[A <: ZoomRange]: Ordering[A] = Ordering.by(_.maxZoom)

  implicit object ZoomRangeFormat extends RootJsonFormat[ZoomRange] {
      def write(zr: ZoomRange) =
        JsObject(
          "minZoom" -> JsNumber(zr.minZoom),
          "maxZoom" -> JsNumber(zr.maxZoom)
        )

      def read(value: JsValue): ZoomRange =
        value.asJsObject.getFields("minZoom", "maxZoom") match {
          case Seq(JsNumber(minZoom), JsNumber(maxZoom)) =>
            ZoomRange(minZoom.toInt, maxZoom.toInt)
          case v =>
            throw new DeserializationException(s"ZoomRange expected, got $v")
        }
    }
}
