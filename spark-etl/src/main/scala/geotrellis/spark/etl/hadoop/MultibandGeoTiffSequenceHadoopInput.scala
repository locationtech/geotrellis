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
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.etl.config.EtlConf
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class MultibandGeoTiffSequenceHadoopInput extends HadoopInput[ProjectedExtent, MultibandTile] {
  val format = "multiband-geotiff-sequence"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    val inputCrs = conf.input.getCrs
    sc
      .sequenceFile[String, Array[Byte]](getPath(conf.input.backend).path)
      .map { case (path, bytes) =>
        val geotiff = GeoTiffReader.readMultiband(bytes)
        (ProjectedExtent(geotiff.extent, inputCrs.getOrElse(geotiff.crs)), geotiff.tile)
      }
  }
}
