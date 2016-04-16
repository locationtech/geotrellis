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
import geotrellis.raster.histogram._

import spray.json._


trait HistogramJsonFormats {
  implicit object HistogramIntFormat extends RootJsonFormat[Histogram[Int]] {
    def write(h: Histogram[Int]): JsValue = {
      var pairs: List[JsArray] = Nil
      h.foreach{ (value, count) => pairs = JsArray(JsNumber(value), JsNumber(count)) :: pairs }
      JsArray(pairs:_*)
    }

    def read(json: JsValue): FastMapHistogram = json match {
      case JsArray(pairs) =>
        val hist = FastMapHistogram()
        for(pair <- pairs) {
          pair match {
            case JsArray(Vector(JsNumber(item), JsNumber(count))) =>  hist.countItem(item.toInt, count.toInt)
            case _ => throw new DeserializationException("Array of [value, count] pairs expected")
          }
        }
        hist
      case _ =>
        throw new DeserializationException("Array of [value, count] pairs expected")
    }
  }

  implicit object HistogramDoubleFormat extends RootJsonFormat[Histogram[Double]] {

    def write(h: Histogram[Double]): JsValue = {
      var buckets: List[JsArray] = Nil
      h.foreach{ (value, count) => buckets = JsArray(JsNumber(value), JsNumber(count)) :: buckets }

      val min = h.minValue match {
        case None => JsString("none")
        case Some(x) => JsNumber(x)
      }

      val max = h.maxValue match {
        case None => JsString("none")
        case Some(x) => JsNumber(x)
      }

      JsObject(
        "buckets" -> JsArray(buckets:_*),
        "maxBucketCount" -> JsNumber(h.maxBucketCount),
        "minimum" -> min,
        "maximum" -> max
      )
    }

    def read(json: JsValue): Histogram[Double] = {
      json.asJsObject.getFields("buckets", "maxBucketCount", "minimum", "maximum") match {

        // A Histogram that contains something
        case Seq(JsArray(bucketArray), JsNumber(maxBucketCount), JsNumber(min), JsNumber(max)) =>
          val histogram = StreamingHistogram(maxBucketCount.toInt, min.toDouble, max.toDouble)

          bucketArray.foreach({ pair =>
            pair match {
              case JsArray(Vector(JsNumber(label), JsNumber(count))) =>
                histogram.countItem(label.toDouble, count.toLong)
              case _ =>
                throw new DeserializationException("Array of [label, count] pairs expected")
            }
          })

          histogram
        // A Histogram that contains nothing
        case Seq(JsArray(bucketArray), JsNumber(maxBucketCount), JsString(_), JsString(_)) =>
          StreamingHistogram(maxBucketCount.toInt)

        // Parsing error
        case _ => throw new DeserializationException("Histogram[Double] expected")
      }
    }

  }

}
