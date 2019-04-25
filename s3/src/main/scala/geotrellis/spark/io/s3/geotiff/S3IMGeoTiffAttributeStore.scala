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

package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.util.annotations.experimental

import spray.json._
import spray.json.DefaultJsonProtocol._
// import software.amazon.awssdk.services.s3.AmazonS3URI
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.io.ByteArrayInputStream
import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3IMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean,
    getS3Client: () => S3Client
  ): InMemoryGeoTiffAttributeStore =
    apply(() => S3GeoTiffInput.list(name, uri, pattern, recursive, getS3Client), getS3Client)

  def apply(
    name: String,
    uri: URI,
    pattern: String
  ): InMemoryGeoTiffAttributeStore = apply(name, uri, pattern, true, () =>
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    S3Client.create())

  def apply(getDataFunction: () => List[GeoTiffMetadata]): InMemoryGeoTiffAttributeStore =
    apply(getDataFunction, () =>
        // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
        S3Client.create())

  def apply(
    getDataFunction: () => List[GeoTiffMetadata],
    getS3Client: () => S3Client
  ): InMemoryGeoTiffAttributeStore =
    new InMemoryGeoTiffAttributeStore {
      lazy val metadataList = getDataFunction()
      def persist(uri: URI): Unit = {
        val s3Client = getS3Client()
        val s3Path = ??? //new AmazonS3URI(uri)
        val bucket = ???
        val key = ???
        val data = metadataList

        val str = data.toJson.compactPrint
        val request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build()
        s3Client.putObject(request, RequestBody.fromBytes(str.getBytes("UTF-8")))
      }
    }
}
