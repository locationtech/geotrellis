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
import geotrellis.proj4.CRS

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import software.amazon.awssdk.services.s3.S3Client

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter


@deprecated("TemporalGeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
object TemporalGeoTiffS3InputFormat {
  final val GEOTIFF_TIME_TAG_DEFAULT = "GEOTIFF_TIME_TAG"
  final val GEOTIFF_TIME_FORMAT_DEFAULT = "GEOTIFF_TIME_FORMAT"

  def setTimeTag(job: JobContext, timeTag: String): Unit =
    setTimeTag(job.getConfiguration, timeTag)

  def setTimeTag(conf: Configuration, timeTag: String): Unit =
    conf.set(GEOTIFF_TIME_TAG_DEFAULT, timeTag)

  def setTimeFormat(job: JobContext, timeFormat: String): Unit =
    setTimeFormat(job.getConfiguration, timeFormat)

  def setTimeFormat(conf: Configuration, timeFormat: String): Unit =
    conf.set(GEOTIFF_TIME_FORMAT_DEFAULT, timeFormat)

  def getTimeTag(job: JobContext) =
    job.getConfiguration.get(GEOTIFF_TIME_TAG_DEFAULT, "TIFFTAG_DATETIME")

  def getTimeFormatter(job: JobContext): DateTimeFormatter = {
    val df = job.getConfiguration.get(GEOTIFF_TIME_FORMAT_DEFAULT)
    (if (df == null) { DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss") }
    else { DateTimeFormatter.ofPattern(df) }).withZone(ZoneOffset.UTC)
  }
}

/** Read single band GeoTiff from S3
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""yyyy:MM:dd HH:mm:ss""
  */
@deprecated("TemporalGeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class TemporalGeoTiffS3InputFormat extends S3InputFormat[TemporalProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new TemporalGeoTiffS3RecordReader(getS3Client(context), context)
}

@deprecated("TemporalGeoTiffS3RecordReader is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class TemporalGeoTiffS3RecordReader(s3Client: S3Client, context: TaskAttemptContext) extends S3RecordReader[TemporalProjectedExtent, Tile](s3Client) {
  val timeTag = TemporalGeoTiffS3InputFormat.getTimeTag(context)
  val dateFormatter = TemporalGeoTiffS3InputFormat.getTimeFormatter(context)

  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)

    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val dateTime = ZonedDateTime.from(dateFormatter.parse(dateTimeString))
    val inputCrs = GeoTiffS3InputFormat.getCrs(context)

    //WARNING: Assuming this is a single band GeoTiff
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (TemporalProjectedExtent(extent, inputCrs.getOrElse(crs), dateTime), tile)
  }
}
