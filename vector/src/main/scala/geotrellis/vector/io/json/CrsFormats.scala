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
import cats.syntax.either._
import java.net.URI

import geotrellis.proj4.CRS

/** A trait specifying CRS/JSON conversion */
trait CrsFormats {
  implicit val crsEncoder: Encoder[CRS] =
    Encoder.encodeString.contramap[CRS] { _.toProj4String }

  implicit val crsDecoder: Decoder[CRS] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(CRS.fromString(str)).leftMap(_ => "CRS must be a proj4 string.")
    }

  implicit val linkedCRSEncoder: Encoder[LinkedCRS] =
    Encoder.encodeJson.contramap[LinkedCRS] { obj =>
      Json.obj(
        "type" -> "link".asJson,
        "properties" -> Json.obj(
          "href" -> obj.href.toString.asJson,
          "type" -> obj.format.asJson
        )
      )
    }

  implicit val linkedCRSDecoder: Decoder[LinkedCRS] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      c.downField("type").as[String].flatMap {
        case "link" =>
          val properties = c.downField("properties")
          val hrefDecoded = properties.downField("href").as[String]
          val typeDecoded = properties.downField("type").as[String]
          (hrefDecoded, typeDecoded) match {
            case (Right(href), Right(linkType)) => Right(LinkedCRS(new URI(href), linkType))
            case _ => Left("Unable to read 'crs.properties'")
          }
        case crsType => Left(s"Unable to read CRS of type $crsType")
      }.leftMap(_ => "Unable to parse LinkedCRS")
    }

  implicit val namedCRSEncoder: Encoder[NamedCRS] =
    Encoder.encodeJson.contramap[NamedCRS] { obj =>
      Json.obj(
        "type" -> "name".asJson,
        "properties" -> Json.obj("name" -> obj.name.asJson)
      )
    }

  implicit val namedCRSDecoder: Decoder[NamedCRS] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      c.downField("type").as[String].flatMap {
        case "name" =>
          val properties = c.downField("properties")
          val nameDecoded = properties.downField("name").as[String]

          nameDecoded match {
            case (Right(name)) => Right(NamedCRS(name))
            case _ => Left("Unable to read 'crs.properties'")
          }
        case crsType => Left(s"Unable to read CRS of type $crsType")
      }.leftMap(_ => "Unable to parse NamedCRS")
    }

  implicit val jsonCrsEncoder: Encoder[JsonCRS] =
    Encoder.encodeJson.contramap[JsonCRS] {
      case crs: NamedCRS => crs.asJson
      case crs: LinkedCRS => crs.asJson
    }

  implicit val jsonCrsDecoder: Decoder[JsonCRS] = {
    Decoder.decodeHCursor.emap { c: HCursor =>
      c.downField("type").as[String].flatMap {
        case "name" => c.as[NamedCRS]
        case "link" => c.as[LinkedCRS]
        case crsType => Left(s"Unable to read CRS of type $crsType")
      }.leftMap(_ => "Unable to parse CRS")
    }
  }

  implicit def withCrsEncoder[T: Encoder]: Encoder[WithCrs[T]] =
    Encoder.encodeJson.contramap[WithCrs[T]] { withCrs =>
      Json.fromJsonObject(withCrs.obj.asJson.asObject.map(_.add("crs", withCrs.crs.asJson)).getOrElse(throw new Exception))
    }

  implicit def withCrsDecoder[T: Decoder]: Decoder[WithCrs[T]] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.as[T], c.downField("crs").as[JsonCRS]) match {
        case (Right(obj), Right(crs)) => Right(WithCrs[T](obj, crs))
        case _ => Left(s"Unable to parse CRS")
      }
    }
}

object CrsFormats extends CrsFormats
