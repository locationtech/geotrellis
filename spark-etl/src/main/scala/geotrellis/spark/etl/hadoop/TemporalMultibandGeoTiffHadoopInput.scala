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

import geotrellis.raster.MultibandTile
import geotrellis.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalMultibandGeoTiffHadoopInput extends HadoopInput[TemporalProjectedExtent, MultibandTile] {
  val format = "temporal-geotiff"

  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    HadoopGeoTiffRDD.temporalMultiband(
      getPath(conf.input.backend).path,
      HadoopGeoTiffRDD.Options(
        timeTag =
          conf.output.keyIndexMethod.timeTag
            .getOrElse(HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT),
        timeFormat =
          conf.output.keyIndexMethod.timeFormat
            .getOrElse(HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT),
        crs = conf.input.getCrs,
        maxTileSize = conf.input.maxTileSize,
        numPartitions = conf.input.numPartitions
      )
    )
}
