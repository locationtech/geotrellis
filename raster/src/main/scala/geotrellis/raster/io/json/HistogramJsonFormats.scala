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

package geotrellis.raster.io.json

import geotrellis.raster._
import geotrellis.raster.histogram._

import spray.json._

import scala.collection.mutable.ArrayBuffer

trait HistogramJsonFormats {
  implicit object HistogramIntFormat extends RootJsonFormat[Histogram[Int]] {
    def write(h: Histogram[Int]): JsValue = {
      var pairs = ArrayBuffer[JsArray]()
      h.foreach { (value, count) => pairs += JsArray(JsNumber(value), JsNumber(count))}
      JsArray(pairs: _*)
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

    def write(h: Histogram[Double]): JsValue =
      h.minValue.flatMap { min =>
        h.maxValue.map { max => (min, max) }
      } match {
        case Some((min, max)) =>
          var pairs = ArrayBuffer[JsArray]()
          h.foreach { (value, count) => pairs += JsArray(JsNumber(value), JsNumber(count)) }

          JsObject(
            "buckets" -> JsArray(pairs: _*),
            "maxBucketCount" -> JsNumber(h.maxBucketCount),
            "minimum" -> JsNumber(min),
            "maximum" -> JsNumber(max)
          )
        case None => // Empty histogram
          JsObject(
            "maxBucketCount" -> JsNumber(h.maxBucketCount)
          )
      }

    def read(json: JsValue): Histogram[Double] =
      json.asJsObject.getFields("maxBucketCount") match {
        case Seq(JsNumber(maxBucketCount)) =>
          json.asJsObject.getFields("buckets", "minimum", "maximum") match {
            case Seq(JsArray(bucketArray), JsNumber(min), JsNumber(max)) =>
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

            case _ => // Emtpy histogram
              StreamingHistogram(maxBucketCount.toInt)
          }
        // Parsing error
        case _ => throw new DeserializationException("Histogram[Double] expected")
      }
  }
}
