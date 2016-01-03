/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.TaskAttemptContext

class GeotiffInputFormat extends BinaryFileInputFormat[ProjectedExtent, Tile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (ProjectedExtent, Tile) = {
    val ProjectedRaster(Raster(tile, extent), crs) = SingleBandGeoTiff(bytes).projectedRaster
    (ProjectedExtent(extent, crs), tile)
  }
}
