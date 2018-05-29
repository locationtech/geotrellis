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
