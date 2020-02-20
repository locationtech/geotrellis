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

import geotrellis.vector._

import cats.syntax.either._
import _root_.io.circe._
import _root_.io.circe.syntax._
import scala.util.{Try, Success, Failure}

/** A trait for providing the Circe json encoders and decoders necessary to serialize [[Feature]] instances */
trait FeatureFormats {

  /** Serializes a feature object to a GeoJSON feature
    *
    * @param A Feature object
    * @tparam The type (which must have an implicit method to resolve the transformation from json)
    * @return The GeoJson compliant circe Json
    */
  def writeFeatureJson[G <: Geometry, D: Encoder](obj: Feature[G, D]): Json = {
    Json.obj(
      "type" -> "Feature".asJson,
      "geometry"   -> GeometryFormats.geometryEncoder(obj.geom),
      "bbox"       -> Extent.listEncoder(obj.geom.extent),
      "properties" -> obj.data.asJson
    )
  }

  def writeFeatureJsonWithID[G <: Geometry, D: Encoder](idFeature: (String, Feature[G, D])): Json = {
    Json.obj(
      "type"       -> "Feature".asJson,
      "geometry"   -> GeometryFormats.geometryEncoder(idFeature._2.geom),
      "bbox"       -> Extent.listEncoder(idFeature._2.geom.extent),
      "properties" -> idFeature._2.data.asJson,
      "id"         -> idFeature._1.asJson
    )
  }

  def readFeatureJson[D: Decoder, G <: Geometry: Decoder](value: Json): Feature[G, D] = {
    val c = value.hcursor
    (c.downField("type").as[String], c.downField("geometry").focus, c.downField("properties").focus) match {
      case (Right("Feature"), Some(geom), Some(data)) =>
        (geom.as[G].toOption, data.as[D].toOption) match {
          case (Some(g), Some(d)) => Feature(g, d)
          case _ => throw new Exception("Feature expected")
        }

      case _ => throw new Exception("Feature expected")
    }
  }

  def readFeatureJsonWithID[D: Decoder, G <: Geometry: Decoder](value: Json): (String, Feature[G, D]) = {
    val c = value.hcursor
    (c.downField("type").as[String], c.downField("geometry").focus, c.downField("properties").focus, c.downField("id").focus) match {
      case (Right("Feature"), Some(geom), Some(data), Some(id)) =>
        (geom.as[G].toOption, data.as[D].toOption, id.as[String].toOption) match {
          case (Some(g), Some(d), Some(i)) => (i, Feature(g, d))
          case _ => throw new Exception("Feature expected")
        }

      case _ => throw new Exception("Feature expected")
    }
  }

  implicit def featureDecoder[G <: Geometry: Decoder, D: Decoder]: Decoder[Feature[G, D]] =
    Decoder.decodeJson.emap { json: Json =>
      Try(readFeatureJson[D, G](json)) match {
        case Success(f) => Right(f)
        case Failure(e) => Left(e.getMessage)
      } }

  implicit def featureEncoder[G <: Geometry: Encoder, D: Encoder]: Encoder[Feature[G, D]] =
    Encoder.encodeJson.contramap[Feature[G, D]] { writeFeatureJson }

  implicit val featureCollectionEncoder: Encoder[JsonFeatureCollection] =
    Encoder.encodeJson.contramap[JsonFeatureCollection] { _.asJson }

  implicit val featureCollectionDecoder: Decoder[JsonFeatureCollection] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("features").focus) match {
        case (Right("FeatureCollection"), Some(features)) => Right(JsonFeatureCollection(features.asArray.toVector.flatten))
        case _ => Left("FeatureCollection expected")
      }
    }

  implicit val featureCollectionMapEncoder: Encoder[JsonFeatureCollectionMap] =
    Encoder.encodeJson.contramap[JsonFeatureCollectionMap] { _.asJson }

  implicit val featureCollectionMapDecoder: Decoder[JsonFeatureCollectionMap] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("features").focus) match {
        case (Right("FeatureCollection"), Some(features)) => Right(JsonFeatureCollectionMap(features.asArray.toVector.flatten))
        case _ => Left("FeatureCollection expected")
      }
    }
}

object FeatureFormats extends FeatureFormats
