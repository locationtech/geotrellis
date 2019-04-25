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

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, PutObjectRequest}
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.ByteArrayInputStream
import java.net.URI

import scala.io.Source

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3JsonGeoTiffAttributeStore {
  @experimental def readData(uri: URI, getS3Client: () => S3Client = () =>
      // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
      S3Client.create()): List[GeoTiffMetadata] = {
    val s3Client = getS3Client()
    val s3Uri = ???// new AmazonS3URI(uri)
    val bucket = ???
    val key = ???
    val request = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    val objStream =
      s3Client.getObject(request)

    val json = try {
      Source
        .fromInputStream(objStream)
        .getLines
        .mkString(" ")
    } finally objStream.close()

    json
      .parseJson
      .convertTo[List[GeoTiffMetadata]]
  }

  @experimental def readDataAsTree(uri: URI, getS3Client: () => S3Client): GeoTiffMetadataTree[GeoTiffMetadata] =
    GeoTiffMetadataTree.fromGeoTiffMetadataSeq(readData(uri, getS3Client))

  def apply(uri: URI): JsonGeoTiffAttributeStore = {
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    JsonGeoTiffAttributeStore(uri, readDataAsTree(_, () => S3Client.create()))
  }

  def apply(
    path: URI,
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    getS3Client: () => S3Client = () =>
      // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
      S3Client.create()
  ): JsonGeoTiffAttributeStore = {
    val s3Client = getS3Client()
    val s3Path = ???//new AmazonS3URI(path)
    val bucket = ???
    val key = ???
    val data = S3GeoTiffInput.list(name, uri, pattern, recursive)
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    val attributeStore = JsonGeoTiffAttributeStore(path, readDataAsTree(_, () => S3Client.create()))

    val str = data.toJson.compactPrint
    val request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    s3Client.putObject(request, RequestBody.fromBytes(str.getBytes("UTF-8")))

    attributeStore
  }
}
