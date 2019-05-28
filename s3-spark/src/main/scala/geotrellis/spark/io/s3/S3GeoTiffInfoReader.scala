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

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.store.s3._
import geotrellis.store.s3.util.S3RangeReader
import geotrellis.spark.store._
import geotrellis.util.ByteReader

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._

case class S3GeoTiffInfoReader(
  bucket: String,
  prefix: String,
  getClient: () => S3Client = S3ClientProducer.get,
  delimiter: Option[String] = None,
  streaming: Boolean = true,
  tiffExtensions: Seq[String] = S3GeoTiffRDD.Options.DEFAULT.tiffExtensions
) extends GeoTiffInfoReader {

  @transient
  lazy val client = getClient()

  /** Returns RDD of URIs to tiffs as GeoTiffInfo is not serializable. */
  def geoTiffInfoRDD(implicit sc: SparkContext): RDD[String] = {
    val listObjectsRequest =
      delimiter match {
        case Some(d) =>
          ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .delimiter(d)
            .build()
        case None =>
          ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .build()
      }

    val s3objects =
      client
        .listObjectsV2(listObjectsRequest)
        .contents
        .asScala
    val s3keys = s3objects.map(_.key)
    sc.parallelize(s3keys)
      .flatMap(key => if(tiffExtensions.exists(key.endsWith)) Some(s"s3://$bucket/$key") else None)
  }

  def getGeoTiffInfo(uri: String): GeoTiffInfo = {
    val s3Uri = new AmazonS3URI(uri)
    val bucket = s3Uri.getBucket()
    val key = s3Uri.getKey()
    val ovrKey = s"${s3Uri.getKey}.ovr"
    val ovrReader: Option[ByteReader] =
      if (client.objectExists(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, client))
      else None

    val s3rr = S3RangeReader(bucket, key, client)
    GeoTiffReader.readGeoTiffInfo(s3rr, streaming, true, ovrReader)
  }
}

object S3GeoTiffInfoReader {
  def apply(
    bucket: String,
    prefix: String,
    options: S3GeoTiffRDD.Options
  ): S3GeoTiffInfoReader =
    S3GeoTiffInfoReader(bucket, prefix, options.getClient, options.delimiter, tiffExtensions = options.tiffExtensions)
}
