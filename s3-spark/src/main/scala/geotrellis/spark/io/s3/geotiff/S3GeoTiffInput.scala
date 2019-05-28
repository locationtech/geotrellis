/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.store.s3.geotiff

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.store.s3.{S3ClientProducer, AmazonS3URI}
import geotrellis.store.s3.util.S3RangeReader
import geotrellis.spark.store.hadoop.geotiff.GeoTiffMetadata
import geotrellis.util.StreamingByteReader
import geotrellis.util.annotations.experimental

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

import scala.collection.JavaConverters._
import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3GeoTiffInput {

  /**
    * Returns a list of URIs matching given regexp
    */
  @experimental def list(
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    getClient: () => S3Client = S3ClientProducer.get
  ): List[GeoTiffMetadata] = {

    @transient
    lazy val s3Client = getClient()

    val s3Uri = new AmazonS3URI(uri)
    val regexp = pattern.r

    val objectRequest = if (recursive) {
      ListObjectsV2Request.builder()
        .bucket(s3Uri.getBucket())
        .prefix(s3Uri.getKey())
        .build()
    } else {
      ListObjectsV2Request.builder()
        .bucket(s3Uri.getBucket())
        .prefix(s3Uri.getKey())
        .delimiter("/")
        .build()
    }

    s3Client.listObjectsV2Paginator(objectRequest)
      .contents
      .asScala
      .flatMap({ s3obj =>
        s3obj.key match {
          case regexp(_*) => Some((s3Uri.getBucket(), s3obj.key))
          case _ => None
        }
      }).map({ bucketAndKey: (String, String) =>
        val tiffTags = TiffTagsReader.read(StreamingByteReader(
          S3RangeReader(
            bucket = bucketAndKey._1,
            key = bucketAndKey._2,
            client = s3Client
          )
        ))

        val uri = new AmazonS3URI(s"s3://${bucketAndKey._1}/${bucketAndKey._2}").getURI()
        GeoTiffMetadata(tiffTags.extent, tiffTags.crs, name, uri)
      })
      .toList
  }
}
