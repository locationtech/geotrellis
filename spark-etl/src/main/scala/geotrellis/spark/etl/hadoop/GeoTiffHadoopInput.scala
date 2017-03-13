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

package geotrellis.spark.etl.hadoop

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.vector.ProjectedExtent
import geotrellis.spark.ingest._
import geotrellis.spark.merge._
import geotrellis.spark.io.hadoop._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffHadoopInput extends HadoopInput[ProjectedExtent, Tile]() {
  val format = "geotiff"

  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    HadoopGeoTiffRDD.spatial(
      getPath(conf.input.backend).path,
      HadoopGeoTiffRDD.Options(
        crs = conf.input.getCrs,
        maxTileSize = conf.input.maxTileSize,
        numPartitions = conf.input.numPartitions
      )
    )
}
