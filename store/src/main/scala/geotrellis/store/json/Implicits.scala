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

package geotrellis.store.json

import geotrellis.layer._
import geotrellis.proj4.CRS
import geotrellis.vector.io.json.CrsFormats

import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import cats.syntax.either._
import org.apache.avro.Schema

import java.time.{ZoneOffset, ZonedDateTime}
import java.net.URI

object Implicits extends Implicits

trait Implicits extends KeyIndexFormats with CrsFormats {
  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[URI] { _.toString }
  implicit val uriDecoder: Decoder[URI] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(URI.create(str)).leftMap(_ => "URI must be a string.")
    }

  implicit val zoomedLayoutSchemeEncoder: Encoder[ZoomedLayoutScheme] =
    Encoder.encodeJson.contramap[ZoomedLayoutScheme] { obj =>
      Json.obj(
        "crs"                 -> obj.crs.asJson,
        "tileSize"            -> obj.tileSize.asJson,
        "resolutionThreshold" -> obj.resolutionThreshold.asJson
      )
    }

  implicit val zoomedLayoutSchemeDecoder: Decoder[ZoomedLayoutScheme] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("crs").as[CRS], c.downField("tileSize").as[Int], c.downField("resolutionThreshold").as[Double]) match {
        case (Right(crs), Right(tileSize), Right(resolutionThreshold)) => Right(ZoomedLayoutScheme(crs, tileSize, resolutionThreshold))
        case _ => Left("ZoomedLayoutScheme expected")
      }
    }

  implicit val floatingLayoutSchemeEncoder: Encoder[FloatingLayoutScheme] =
    Encoder.encodeJson.contramap[FloatingLayoutScheme] { obj =>
      Json.obj(
        "tileCols" -> obj.tileCols.asJson,
        "tileRows" -> obj.tileRows.asJson
      )
    }

  implicit val floatingSchemeDecoder: Decoder[FloatingLayoutScheme] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("tileCols").as[Int], c.downField("tileRows").as[Int]) match {
        case (Right(tileCols), Right(tileRows)) => Right(FloatingLayoutScheme(tileCols, tileRows))
        case _ => Left("FloatingLayoutScheme expected")
      }
    }

  implicit val layoutSchemeEncoder: Encoder[LayoutScheme] =
    Encoder.encodeJson.contramap[LayoutScheme] {
      case scheme: ZoomedLayoutScheme => scheme.asJson
      case scheme: FloatingLayoutScheme => scheme.asJson
      case _ => throw new Exception("ZoomedLayoutScheme or FloatingLayoutScheme expected")
    }

  implicit val layoutSchemeDecoder: Decoder[LayoutScheme] = {
    Decoder.decodeHCursor.emap { c: HCursor =>
      zoomedLayoutSchemeDecoder(c) match {
        case Right(r) => Right(r)
        case _ => floatingSchemeDecoder(c) match {
          case Right(r) => Right(r)
          case _ => Left("ZoomedLayoutScheme or FloatingLayoutScheme expected")
        }
      }
    }
  }

  implicit val zonedDateTimeEncoder: Encoder[ZonedDateTime] =
    Encoder.encodeString.contramap[ZonedDateTime] { _.withZoneSameLocal(ZoneOffset.UTC).toString }
  implicit val zomedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(ZonedDateTime.parse(str)).leftMap(_ => "DateTime expected")
    }

  implicit val schemaEncoder: Encoder[Schema] =
    Encoder.encodeJson.contramap[Schema] { schema => parse(schema.toString).valueOr(throw _) }
  implicit val schemaDecoder: Decoder[Schema] =
    Decoder.decodeJson.emap { json =>
      Either.catchNonFatal((new Schema.Parser).parse(json.noSpaces)).leftMap(_ => "Schema expected")
    }
}
