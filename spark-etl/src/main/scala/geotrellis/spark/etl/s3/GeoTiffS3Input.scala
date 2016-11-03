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

package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.ingest._
import geotrellis.spark.io.s3.{GeoTiffS3InputFormat, TemporalGeoTiffS3InputFormat}
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffS3Input extends S3Input[ProjectedExtent, Tile] {
  val format = "geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val hadoopConfig = configuration(conf.input)
    conf.input.crs.foreach(TemporalGeoTiffS3InputFormat.setCrs(hadoopConfig, _))
    sc.newAPIHadoopRDD(hadoopConfig, classOf[GeoTiffS3InputFormat], classOf[ProjectedExtent], classOf[Tile])
  }
}

