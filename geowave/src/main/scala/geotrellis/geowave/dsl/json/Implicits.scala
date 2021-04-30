/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.dsl.json

import java.net.URI
import java.time.Instant

import cats.syntax.either._
import geotrellis.geowave.adapter.{DataType, DataTypeRegistry}
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.syntax._
import io.estatico.newtype.Coercible
import org.locationtech.geowave.core.geotime.store.dimension.Time.TimeRange
import org.locationtech.geowave.core.index.sfc.data.NumericRange
import org.locationtech.geowave.core.store.query.filter.BasicQueryFilter

trait Implicits {
  implicit val customConfig: Configuration = Configuration.default.withDiscriminator("classType")

  /** Derive circe codecs for newtypes. */
  implicit def coercibleEncoder[R, N](implicit ev: Coercible[Encoder[R], Encoder[N]], R: Encoder[R]): Encoder[N] = ev(R)
  implicit def coercibleDecoder[R, N](implicit ev: Coercible[Decoder[R], Decoder[N]], R: Decoder[R]): Decoder[N] = ev(R)

  /** Overriding auto derived newtype codec to throw a more informative error. */
  implicit val dataTypeDecoder: Decoder[DataType] = Decoder.decodeString.emap { str =>
    DataTypeRegistry
      .find(str)
      .fold(s"Invalid DataType: $str; available DataTypes: ${DataTypeRegistry.supportedTypes.mkString(", ")}".asLeft[DataType])(_.asRight)
  }

  /** Other Circe codecs */
  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap[URI](_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(URI.create(str)).leftMap(_ => s"Could not decode URI: $str")
  }

  implicit val encodeFoo: Encoder[NumericRange] = { numericRange =>
    Json.obj("min" -> numericRange.getMin.asJson, "max" -> numericRange.getMax.asJson)
  }

  implicit val decodeFoo: Decoder[NumericRange] = { c =>
    for {
      min <- c.downField("min").as[Double]
      max <- c.downField("max").as[Double]
    } yield new NumericRange(min, max)
  }

  implicit val encodeTimeRange: Encoder[TimeRange] = { range =>
    val data = range.toNumericData
    Json.obj(
      "min" -> Instant.ofEpochMilli(data.getMin.toLong).asJson,
      "max" -> Instant.ofEpochMilli(data.getMax.toLong).asJson)
  }

  implicit val decodeTimeRange: Decoder[TimeRange] = { c =>
    for {
      min <- c.downField("min").as[Instant]
      max <- c.downField("max").as[Instant]
    } yield new TimeRange(min.toEpochMilli, max.toEpochMilli)
  }

  implicit val basicQueryCompareOperationEncoder: Encoder[BasicQueryFilter.BasicQueryCompareOperation] =
    Encoder.encodeString.contramap[BasicQueryFilter.BasicQueryCompareOperation](_.toString)

  implicit val basicQueryCompareOperationDecoder: Decoder[BasicQueryFilter.BasicQueryCompareOperation] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(BasicQueryFilter.BasicQueryCompareOperation.valueOf(str))
        .leftMap(_ => s"Could not decode BasicQueryCompareOperation: $str")
    }
}

object Implicits extends Implicits
