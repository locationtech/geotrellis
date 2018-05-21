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

package geotrellis.spark.etl.config

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.syntax.either._

import geotrellis.spark.etl.config.json._
import geotrellis.proj4.CRS
import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel

case class Input(
  name: String,
  format: String,
  backend: Backend,
  cache: Option[StorageLevel] = None,
  noData: Option[Double] = None,
  clip: Option[Extent] = None,
  crs: Option[String] = None,
  maxTileSize: Option[Int] = HadoopGeoTiffRDD.Options.DEFAULT.maxTileSize,
  numPartitions: Option[Int] = None,
  partitionBytes: Option[Long] = HadoopGeoTiffRDD.Options.DEFAULT.partitionBytes
) extends Serializable {
  def getCrs = crs.map(CRS.fromName)
}

object Input {
  implicit val inputEncoder: Encoder[Input] =
    Encoder.encodeJson.contramap[Input] { i =>
      Json.obj(
        "name" -> i.name.asJson,
        "format" -> i.format.asJson,
        "backend" -> Backend.backendEncoder(i.backend),
        "cache" -> i.cache.asJson,
        "noData" -> i.noData.asJson,
        "clip" -> i.clip.asJson,
        "crs" -> i.crs.asJson,
        "maxTileSize" -> i.crs.asJson,
        "numPartitions" -> i.numPartitions.asJson,
        "partitionBytes" -> i.partitionBytes.asJson
      )
    }

  case class InputDecoder(bp: Map[String, BackendProfile]) extends Decoder[Input] {
    val bd = Backend.BackendDecoder(bp)
    def apply(c: HCursor): Decoder.Result[Input] = {
      Right(
        Input(
          name    = c.downField("name").as[String].valueOr(throw _),
          format  = c.downField("format").as[String].valueOr(throw _),
          backend = bd(c.downField("backend").focus.map(_.hcursor).get).valueOr(throw _),
          cache   = c.downField("cache").as[StorageLevel].toOption,
          noData  = c.downField("noData").as[Double].toOption,
          clip    = c.downField("clip").as[Extent].toOption,
          crs     = c.downField("crs").as[String].toOption,
          maxTileSize = c.downField("maxTileSize").as[Int].toOption,
          numPartitions = c.downField("numPartitions").as[Int].toOption,
          partitionBytes = c.downField("partitionBytes").as[Long].toOption
        )
      )
    }
  }

  implicit val inputsEncoder: Encoder[List[Input]] = deriveEncoder

  case class InputsDecoder(bp: Map[String, BackendProfile]) extends Decoder[List[Input]] {
    val id = InputDecoder(bp)
    def apply(c: HCursor): Decoder.Result[List[Input]] =
      Right(c.focus.flatMap(_.asArray).toList.flatMap {
        _.flatMap(j => id(j.hcursor).toOption)
      })
  }
}
