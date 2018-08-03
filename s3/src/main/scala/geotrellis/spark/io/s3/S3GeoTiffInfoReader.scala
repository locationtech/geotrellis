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

package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.io._
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.ByteReader

import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.ListObjectsRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class S3GeoTiffInfoReader(
  bucket: String,
  prefix: String,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  delimiter: Option[String] = None,
  streaming: Boolean = true,
  tiffExtensions: Seq[String] = S3GeoTiffRDD.Options.DEFAULT.tiffExtensions
) extends GeoTiffInfoReader {

  /** Returns RDD of URIs to tiffs as GeoTiffInfo is not serializable. */
  def geoTiffInfoRDD(implicit sc: SparkContext): RDD[String] = {
    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    sc.parallelize(getS3Client().listKeys(listObjectsRequest))
      .flatMap(key => if(tiffExtensions.exists(key.endsWith)) Some(s"s3://$bucket/$key") else None)
  }

  def getGeoTiffInfo(uri: String): GeoTiffInfo = {
    val s3Uri = new AmazonS3URI(uri)
    val ovrKey = s"${s3Uri.getKey}.ovr"
    val ovrReader: Option[ByteReader] =
      if (getS3Client().doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, getS3Client())) else None

    GeoTiffReader.readGeoTiffInfo(S3RangeReader(s3Uri.getBucket, s3Uri.getKey, getS3Client()), streaming, true, ovrReader)
  }

  def getGeoTiffTags(uri: String): TiffTags = {
    val s3Uri = new AmazonS3URI(uri)
    val rr = S3RangeReader(s3Uri.getBucket, s3Uri.getKey, getS3Client())
    TiffTags(rr)
  }
}

object S3GeoTiffInfoReader {
  def apply(
    bucket: String,
    prefix: String,
    options: S3GeoTiffRDD.Options
  ): S3GeoTiffInfoReader = S3GeoTiffInfoReader(bucket, prefix, options.getS3Client, options.delimiter, tiffExtensions = options.tiffExtensions)
}
