/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.json

import geotrellis.raster._
import geotrellis.raster.histogram.StreamingHistogram

import spray.json._


trait StreamingHistogramJson {

  implicit object StreamingHistogramFormat extends RootJsonFormat[StreamingHistogram] {

    def write(h: StreamingHistogram): JsValue = {
      val buckets = JsArray(h.buckets.map({ bucket => JsArray(JsNumber(bucket.label), JsNumber(bucket.count)) }): _*)
      val min = h.minValue match {
        case None => JsString("none")
        case Some(x) => JsNumber(x)
      }
      val max = h.maxValue match {
        case None => JsString("none")
        case Some(x) => JsNumber(x)
      }
      val size = JsNumber(h.maxBuckets)

      JsObject(
        "size" -> size,
        "buckets" -> buckets,
        "minimum" -> min,
        "maximum" -> max
      )
    }

    def read(json: JsValue): StreamingHistogram = {
      json.asJsObject.getFields("size", "buckets", "minimum", "maximum") match {

        // A StreamingHistogram that contains something
        case Seq(JsNumber(size), JsArray(bucketArray), JsNumber(min), JsNumber(max)) =>
          val histogram = StreamingHistogram(size.toInt, min.toDouble, max.toDouble)

          bucketArray.foreach({ pair =>
            pair match {
              case JsArray(Vector(JsNumber(label), JsNumber(count))) =>
                histogram.countItem(label.toDouble, count.toLong)
              case _ =>
                throw new DeserializationException("Array of [label, count] pairs expected")
            }
          })
          histogram

        // A StreamingHistogram that contains nothing
        case Seq(JsNumber(size), JsArray(bucketArray), JsString(_), JsString(_)) =>
          StreamingHistogram(size.toInt)

        // Parsing error
        case _ => throw new DeserializationException("StreamingHistogram expected")
      }
    }

  }

}
