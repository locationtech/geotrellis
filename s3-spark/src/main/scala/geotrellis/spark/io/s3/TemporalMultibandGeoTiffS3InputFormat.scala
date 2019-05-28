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
import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.spark._
import org.apache.hadoop.mapreduce._

import java.time.ZonedDateTime

/** Read single band GeoTiff from S3
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalMultibandGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalMultibandGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""yyyy:MM:DD HH:MM:SS""
  */
@deprecated("TemporalMultibandGeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class TemporalMultibandGeoTiffS3InputFormat extends S3InputFormat[TemporalProjectedExtent, MultibandTile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[TemporalProjectedExtent, MultibandTile](getS3Client(context)) {
      def read(key: String, bytes: Array[Byte]) = {
        val geoTiff = MultibandGeoTiff(bytes)

        val timeTag = TemporalGeoTiffS3InputFormat.getTimeTag(context)
        val dateFormatter = TemporalGeoTiffS3InputFormat.getTimeFormatter(context)
        val inputCrs = GeoTiffS3InputFormat.getCrs(context)

        val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
        val dateTime = ZonedDateTime.parse(dateTimeString, dateFormatter)

        val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
        (TemporalProjectedExtent(extent, inputCrs.getOrElse(crs), dateTime), tile)
      }
    }
}
