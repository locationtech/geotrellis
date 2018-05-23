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

package geotrellis.spark.etl.config.json

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.syntax.either._

import geotrellis.raster.resample._
import geotrellis.spark.etl.config._
import geotrellis.spark.io.cassandra.conf._

import org.apache.spark.storage.StorageLevel

trait ConfigFormats {
  implicit val storageLevelEncoder: Encoder[StorageLevel] =
    Encoder.encodeJson.contramap[StorageLevel] {
      case StorageLevel.NONE => "NONE".asJson
      case StorageLevel.DISK_ONLY => "DISK_ONLY".asJson
      case StorageLevel.MEMORY_ONLY => "MEMORY_ONLY".asJson
      case StorageLevel.MEMORY_ONLY_2 => "MEMORY_ONLY_2".asJson
      case StorageLevel.MEMORY_ONLY_SER => "MEMORY_ONLY_SER".asJson
      case StorageLevel.MEMORY_ONLY_SER_2 => "MEMORY_ONLY_SER_2".asJson
      case StorageLevel.MEMORY_AND_DISK => "MEMORY_ONLY_DISK".asJson
      case StorageLevel.MEMORY_AND_DISK_2 => "MEMORY_ONLY_DISK_2".asJson
      case StorageLevel.MEMORY_AND_DISK_SER => "MEMORY_ONLY_DISK_SER".asJson
      case StorageLevel.MEMORY_AND_DISK_SER_2 => "MEMORY_ONLY_DISK_SER_2".asJson
      case StorageLevel.OFF_HEAP => "OFF_HEAP".asJson
      case sl => throw new IllegalArgumentException(s"Invalid StorageLevel: $sl")
    }

  implicit val storageLevelDecoder: Decoder[StorageLevel] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(StorageLevel.fromString(str)).leftMap(_ => "StorageLevel must be a valid string.")
    }


  implicit val pointResampleMethodEncoder: Encoder[PointResampleMethod] =
    Encoder.encodeJson.contramap[PointResampleMethod] {
      case NearestNeighbor  => "nearest-neighbor".asJson
      case Bilinear         => "bilinear".asJson
      case CubicConvolution => "cubic-convolution".asJson
      case CubicSpline      => "cubic-spline".asJson
      case Lanczos          => "lanczos".asJson
    }

  implicit val pointResampleMethodDecoder: Decoder[PointResampleMethod] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(str match {
        case "nearest-neighbor" => NearestNeighbor
        case "bilinear" => Bilinear
        case "cubic-convolution" => CubicConvolution
        case "cubic-spline" => CubicSpline
        case "lanczos" => Lanczos
      }).leftMap(_ => "PointResampleMethod must be a valid string.")
    }

  implicit val backendProfilesDecoder: Decoder[Map[String, BackendProfile]] =
    Decoder.decodeHCursor.emap { c =>
      c.downField("backend-profiles").values match {
        case Some(bp) =>
          Right(bp.flatMap { js =>
            val nc = js.hcursor
            ((nc.downField("name").as[String], nc.downField("type").as[String]) match {
              case (Right(n), Right(t)) => (BackendType.fromString(t) match {
                case HadoopType => js.as[HadoopProfile]
                case S3Type => js.as[S3Profile]
                case AccumuloType => js.as[AccumuloProfile]
                case CassandraType => js.as[CassandraProfile]
                case HBaseType => js.as[HBaseProfile]
              }).map(n -> _)
              case _ => Left("BackendProfiles must be a valid json object.")
            }).toOption
          }.toMap)

        case _ => Left("BackendProfiles must be a valid json object.")
      }
    }

  implicit val cassandraCollectionEncoder: Encoder[CassandraCollectionConfig] = deriveEncoder
  implicit val cassandraCollectionDecoder: Decoder[CassandraCollectionConfig] = deriveDecoder

  implicit val cassandraRDDConfigEncoder: Encoder[CassandraRDDConfig] = deriveEncoder
  implicit val cassandraRDDConfigDecoder: Decoder[CassandraRDDConfig] = deriveDecoder

  implicit val cassandraThreadsConfigEncoder: Encoder[CassandraThreadsConfig] = deriveEncoder
  implicit val cassandraThreadsConfigDecoder: Decoder[CassandraThreadsConfig] = deriveDecoder

  implicit val cassandraConfigEncoder: Encoder[CassandraConfig] = deriveEncoder
  implicit val cassandraConfigDecoder: Decoder[CassandraConfig] = deriveDecoder
}
