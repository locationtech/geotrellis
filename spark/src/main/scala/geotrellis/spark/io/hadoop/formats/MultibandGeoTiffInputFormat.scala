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

package geotrellis.spark.io.hadoop.formats

import geotrellis.layers.hadoop.formats.BinaryFileInputFormat
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.TaskAttemptContext

@deprecated("MultibandGeoTiffInputFormat is deprecated, use HadoopGeoTiffRDD instead", "1.0.0")
class MultibandGeoTiffInputFormat extends BinaryFileInputFormat[ProjectedExtent, MultibandTile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (ProjectedExtent, MultibandTile) = {
    val inputCrs = GeoTiffInputFormat.getCrs(context)
    val gt = MultibandGeoTiff(bytes)
    (ProjectedExtent(gt.extent, inputCrs.getOrElse(gt.crs)), gt.tile)
  }
}
