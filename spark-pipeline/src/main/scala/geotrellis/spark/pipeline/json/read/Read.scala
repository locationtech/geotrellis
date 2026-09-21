/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.pipeline.json.read

import geotrellis.spark.store.hadoop.HadoopGeoTiffRDD
import geotrellis.spark.pipeline.json.*


import java.net.URI
import io.circe.{Decoder, Encoder}
import geotrellis.spark.pipeline.json.CodecUtils.orDefault

trait Read extends PipelineExpr {
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]

  def getURI = new URI(uri)
  def getTag = tag.getOrElse("default")
}

case class JsonRead(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: PipelineExprType
) extends Read

object JsonRead {
  implicit val jsonReadEncoder: Encoder[JsonRead] =
    Encoder.forProduct11(
      "uri", "crs", "tag", "max_tile_size", "partitions", "partition_bytes",
      "chunk_size", "delimiter", "time_tag", "time_format", "type"
    ) { r =>
      (r.uri, r.crs, r.tag, r.maxTileSize, r.partitions, r.partitionBytes,
       r.chunkSize, r.delimiter, r.timeTag, r.timeFormat, r.`type`)
    }

  implicit val jsonReadDecoder: Decoder[JsonRead] = Decoder.instance { c =>
    for {
      uri <- c.get[String]("uri")
      crs <- orDefault[Option[String]](c, "crs", None)
      tag <- orDefault[Option[String]](c, "tag", None)
      mts <- orDefault[Option[Int]](c, "max_tile_size", None)
      prt <- orDefault[Option[Int]](c, "partitions", None)
      pby <- orDefault[Option[Long]](c, "partition_bytes", None)
      chs <- orDefault[Option[Int]](c, "chunk_size", None)
      dlm <- orDefault[Option[String]](c, "delimiter", None)
      tt  <- orDefault[String](c, "time_tag", HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT)
      tf  <- orDefault[String](c, "time_format", HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT)
      tpe <- c.get[PipelineExprType]("type")
    } yield JsonRead(uri, crs, tag, mts, prt, pby, chs, dlm, tt, tf, tpe)
  }
}
