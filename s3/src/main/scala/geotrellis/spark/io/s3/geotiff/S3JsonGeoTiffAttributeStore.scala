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

import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.util.annotations.experimental

import io.circe.syntax._
import io.circe.parser._
import cats.syntax.either._
import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.ObjectMetadata

import java.io.ByteArrayInputStream
import java.net.URI

import scala.io.Source

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3JsonGeoTiffAttributeStore {
  @experimental def readData(uri: URI, getS3Client: () => S3Client = () => S3Client.DEFAULT): List[GeoTiffMetadata] = {
    val s3Client = getS3Client()
    val s3Uri = new AmazonS3URI(uri)

    val stream =
      s3Client
        .getObject(s3Uri.getBucket, s3Uri.getKey)
        .getObjectContent

    val json = try {
      Source
        .fromInputStream(stream)
        .getLines
        .mkString(" ")
    } finally stream.close()

    parse(json).flatMap(_.as[List[GeoTiffMetadata]]).valueOr(throw _)
  }

  @experimental def readDataAsTree(uri: URI, getS3Client: () => S3Client): GeoTiffMetadataTree[GeoTiffMetadata] =
    GeoTiffMetadataTree.fromGeoTiffMetadataSeq(readData(uri, getS3Client))

  def apply(uri: URI): JsonGeoTiffAttributeStore =
    JsonGeoTiffAttributeStore(uri, readDataAsTree(_, () => S3Client.DEFAULT))

  def apply(
    path: URI,
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ): JsonGeoTiffAttributeStore = {
    val s3Client = getS3Client()
    val s3Path = new AmazonS3URI(path)
    val data = S3GeoTiffInput.list(name, uri, pattern, recursive)
    val attributeStore = JsonGeoTiffAttributeStore(path, readDataAsTree(_, () => S3Client.DEFAULT))

    val str = data.asJson.noSpaces
    val is = new ByteArrayInputStream(str.getBytes("UTF-8"))
    s3Client.putObject(s3Path.getBucket, s3Path.getKey, is, new ObjectMetadata())

    attributeStore
  }
}
