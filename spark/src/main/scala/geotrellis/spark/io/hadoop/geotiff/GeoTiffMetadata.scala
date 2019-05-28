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

package geotrellis.spark.io.hadoop.geotiff

import geotrellis.proj4.CRS
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.layers._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.annotations.experimental

import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  * Row in a table
  */
@experimental case class GeoTiffMetadata(
  extent: Extent,
  crs: CRS,
  name: String, // name of the ingest data set, by default each tile a separate layer
  uri: URI
) {
  @experimental def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoTiffMetadata {
  implicit val geoTiffMetadataFormat  = jsonFormat4(GeoTiffMetadata.apply)
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental case class TemporalGeoTiffMetadata(
  extent: Extent,
  time: Long,
  crs: CRS,
  name: String,
  uri: URI
) {
  @experimental def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
  @experimental def temporalProjectedExtent: TemporalProjectedExtent = TemporalProjectedExtent(extent, crs, time)
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object TemporalGeoTiffMetadata {
  implicit val temporalGeoTiffMetadataFormat  = jsonFormat5(TemporalGeoTiffMetadata.apply)
}
