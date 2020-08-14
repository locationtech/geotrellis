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

package geotrellis.spark.store.hadoop.geotiff

import geotrellis.proj4.CRS
import geotrellis.vector._
import geotrellis.layer._
import geotrellis.util.annotations.experimental
import geotrellis.spark.{Implicits => SparkImplicits}

import _root_.io.circe._
import _root_.io.circe.generic.semiauto._

import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  * Row in a table
  */
@experimental
case class GeoTiffMetadata(
  extent: Extent,
  crs: CRS,
  name: String, // name of the ingest data set, by default each tile a separate layer
  uri: URI
) {
  @experimental def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
}

object GeoTiffMetadata extends SparkImplicits {
  implicit val GeoTiffMetadataEncoder: Encoder[GeoTiffMetadata] = deriveEncoder
  implicit val GeoTiffMetadataDecoder: Decoder[GeoTiffMetadata] = deriveDecoder
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental
case class TemporalGeoTiffMetadata(
  extent: Extent,
  time: Long,
  crs: CRS,
  name: String,
  uri: URI
) {
  @experimental def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
  @experimental def temporalProjectedExtent: TemporalProjectedExtent = TemporalProjectedExtent(extent, crs, time)
}

object TemporalGeoTiffMetadata extends SparkImplicits {
  implicit val TemporalGeoTiffMetadataEncoder: Encoder[TemporalGeoTiffMetadata] = deriveEncoder
  implicit val TemporalGeoTiffMetadataDecoder: Decoder[TemporalGeoTiffMetadata] = deriveDecoder
}
