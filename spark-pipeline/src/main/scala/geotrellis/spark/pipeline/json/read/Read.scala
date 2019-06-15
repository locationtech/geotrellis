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
import geotrellis.spark.pipeline.json._

import io.circe.generic.extras.ConfiguredJsonCodec

import java.net.URI

trait Read extends PipelineExpr {
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]

  def getURI = new URI(uri)
  def getTag = tag.getOrElse("default")
}

@ConfiguredJsonCodec
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

