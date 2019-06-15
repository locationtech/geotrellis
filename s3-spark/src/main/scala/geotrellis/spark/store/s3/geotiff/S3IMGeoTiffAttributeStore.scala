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

import geotrellis.store.s3.{S3ClientProducer, AmazonS3URI}
import geotrellis.spark.store.hadoop.geotiff._
import geotrellis.util.annotations.experimental

import spray.json._
import spray.json.DefaultJsonProtocol._

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
    recursive: Boolean = true,
    getClient: () => S3Client = S3ClientProducer.get
  ): InMemoryGeoTiffAttributeStore = {
    val getInput = () => S3GeoTiffInput.list(name, uri, pattern, recursive, getClient)
    apply(getInput, getClient)
  }

  def apply(getDataFunction: () => List[GeoTiffMetadata]): InMemoryGeoTiffAttributeStore =
    apply(getDataFunction, S3ClientProducer.get)

  def apply(
    getDataFunction: () => List[GeoTiffMetadata],
    getClient: () => S3Client
  ): InMemoryGeoTiffAttributeStore =
    new InMemoryGeoTiffAttributeStore {
      lazy val metadataList = getDataFunction()
      def persist(uri: URI): Unit = {

        @transient
        lazy val s3Client = getClient()

        val s3Path = new AmazonS3URI(uri)
        val data = metadataList

        val str = data.toJson.compactPrint
        val request = PutObjectRequest.builder()
          .bucket(s3Path.getBucket())
          .key(s3Path.getKey())
          .build()
        s3Client.putObject(request, RequestBody.fromBytes(str.getBytes("UTF-8")))
      }
    }
}
