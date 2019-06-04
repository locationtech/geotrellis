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

package geotrellis.spark.store.hadoop.formats

import geotrellis.proj4.CRS
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.layers.hadoop._
import geotrellis.layers.hadoop.formats.BinaryFileInputFormat
import geotrellis.spark.store.hadoop._
import geotrellis.spark.ingest._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._

@deprecated("GeoTiffInputFormat is deprecated, use HadoopGeoTiffRDD instead", "1.0.0")
object GeoTiffInputFormat {
  final val GEOTIFF_CRS = "GEOTIFF_CRS"

  def setCrs(conf: Configuration, crs: CRS): Unit =
    conf.setSerialized(GEOTIFF_CRS, crs)

  def getCrs(job: JobContext): Option[CRS] =
    job.getConfiguration.getSerializedOption[CRS](GEOTIFF_CRS)
}

@deprecated("GeoTiffInputFormat is deprecated, use HadoopGeoTiffRDD instead", "1.0.0")
class GeoTiffInputFormat extends BinaryFileInputFormat[ProjectedExtent, Tile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (ProjectedExtent, Tile) = {
    val inputCrs = GeoTiffInputFormat.getCrs(context)

    val ProjectedRaster(Raster(tile, extent), crs) = SinglebandGeoTiff(bytes).projectedRaster
    (ProjectedExtent(extent, inputCrs.getOrElse(crs)), tile)
  }
}
