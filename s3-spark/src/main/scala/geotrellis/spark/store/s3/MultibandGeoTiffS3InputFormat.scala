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

package geotrellis.spark.store.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read multi band GeoTiff from S3 */
@deprecated("MultibandGeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class MultibandGeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, MultibandTile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[ProjectedExtent, MultibandTile](getS3Client(context)) {
      def read(key: String, bytes: Array[Byte]) = {
        val geoTiff = MultibandGeoTiff(bytes)
        val inputCrs = GeoTiffS3InputFormat.getCrs(context)
        val projectedExtent = ProjectedExtent(geoTiff.extent, inputCrs.getOrElse(geoTiff.crs))
        (projectedExtent, geoTiff.tile)
      }
    }
}
