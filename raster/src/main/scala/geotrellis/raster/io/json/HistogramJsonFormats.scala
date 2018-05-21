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

import io.circe._
import io.circe.syntax._
import cats.implicits._

import geotrellis.raster.histogram._

import scala.collection.mutable.ArrayBuffer

trait HistogramJsonFormats {
  implicit val histogramIntEncoder: Encoder[Histogram[Int]] =
    Encoder.encodeJson.contramap[Histogram[Int]] { h =>
      var pairs = ArrayBuffer[Json]()
      h.foreach { (value, count) => pairs += Vector(value, count).asJson }
      Json.fromValues(pairs)
    }

  implicit val histogramIntDecoder: Decoder[Histogram[Int]] =
    Decoder.decodeJson.emap { json: Json =>
      Right(json.asArray match {
        case Some(pairs) =>
          val hist = FastMapHistogram()
          for(pair <- pairs) {
            pair.as[Vector[Int]] match {
              case Right(Vector(item, count)) =>
                hist.countItem(item, count)
              case Left(e) => throw e
            }
          }
          hist

        case _ => throw new Exception("Array of [label, count] pairs expected")
      })
    }

  implicit val histogramDoubleEncoder: Encoder[Histogram[Double]] =
    Encoder.encodeJson.contramap[Histogram[Double]] { h =>
      h.minValue.flatMap { min =>
        h.maxValue.map { max => (min, max) }
      } match {
        case Some((min, max)) =>
          var pairs = ArrayBuffer[Json]()
          h.foreach { (value, count) => pairs += Vector(value, count.toDouble).asJson }
          Json.obj(
            "buckets" -> pairs.asJson,
            "maxBucketCount" -> h.maxBucketCount.asJson,
            "minimum" -> min.asJson,
            "maximum" -> max.asJson
          )

        case None => // Empty histogram
          Json.obj(
            "maxBucketCount" -> h.maxBucketCount.asJson
          )
      }
    }

  implicit val histogramDoubleDecoder: Decoder[Histogram[Double]] =
    Decoder.decodeHCursor.emap { hcursor: HCursor =>
      hcursor.downField("maxBucketCount").as[Int] match {
        case Right(maxBucketCount) =>
          val min: Double = hcursor.downField("minBucketCount").as[Double].getOrElse(Double.NegativeInfinity)
          val max: Double = hcursor.downField("maxBucketCount").as[Double].getOrElse(Double.PositiveInfinity)
          val buckets = hcursor.downField("buckets").values.get.map(_.as[(Double, Double)].toOption).toList.sequence.getOrElse(List())

          val histogram = StreamingHistogram(maxBucketCount, min, max)
          buckets.foreach({ case (label, count) => histogram.countItem(label, count.toLong) })
          Right(histogram)
        case Left(err) =>
          Left(err.toString)
      }
    }

  implicit val streamingHistogramEncoder: Encoder[StreamingHistogram] =
    Encoder[Histogram[Double]].contramap[StreamingHistogram](_.asInstanceOf[Histogram[Double]])

  implicit val streamingHistogramDecoder: Decoder[StreamingHistogram] =
    Decoder[Histogram[Double]].map[StreamingHistogram](_.asInstanceOf[StreamingHistogram])

}
