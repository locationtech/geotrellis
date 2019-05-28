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

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark.store.hadoop._
import geotrellis.vector._

import software.amazon.awssdk.services.s3.S3Client
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._

@deprecated("GeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
object GeoTiffS3InputFormat {
  final val GEOTIFF_CRS = "GEOTIFF_CRS"

  def setCrs(job: Job, crs: CRS): Unit =
    setCrs(job.getConfiguration, crs)

  def setCrs(conf: Configuration, crs: CRS): Unit =
    conf.setSerialized(GEOTIFF_CRS, crs)

  def getCrs(job: JobContext): Option[CRS] =
    job.getConfiguration.getSerializedOption[CRS](GEOTIFF_CRS)
}

/** Read single band GeoTiff from S3 */
@deprecated("GeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new GeoTiffS3RecordReader(getS3Client(context), context)
}

@deprecated("GeoTiffS3RecordReader is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class GeoTiffS3RecordReader(s3Client: S3Client, context: TaskAttemptContext) extends S3RecordReader[ProjectedExtent, Tile](s3Client) {
  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)
    val inputCrs = GeoTiffS3InputFormat.getCrs(context)
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (ProjectedExtent(extent, inputCrs.getOrElse(crs)), tile)
  }
}
