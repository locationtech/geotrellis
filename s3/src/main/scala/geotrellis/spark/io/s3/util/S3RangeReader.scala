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

package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import geotrellis.util.RangeReader

import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model._

import java.net.URI

/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff on the
 * AWS S3 server.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @return A new instance of S3RangeReader.
 */
class S3RangeReader(
  request: GetObjectRequest,
  client: S3Client) extends RangeReader {

  val metadata: ObjectMetadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  val totalLength: Long = metadata.getContentLength

  def readClippedRange(start: Long, length: Int): Array[Byte] =
    client.readRange(start, start + length, request)
}

/** The companion object of [[S3RangeReader]] */
object S3RangeReader {

  def apply(s3address: String): S3RangeReader =
    apply(new URI(s3address), AmazonS3Client())

  def apply(s3address: String, client: S3Client): S3RangeReader =
    apply(new URI(s3address), client)

  def apply(uri: URI): S3RangeReader =
    apply(uri, AmazonS3Client())

  def apply(uri: URI, client: S3Client): S3RangeReader = {
    val s3uri = new AmazonS3URI(uri)
    apply(new GetObjectRequest(s3uri.getBucket, s3uri.getKey), client)
  }

  /**
   * Returns a new instance of S3RangeReader.
   *
   * @param bucket: A string that is the name of the bucket.
   * @param key: A string that is the path to the GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @return A new instance of S3RangeReader.
   */
  def apply(bucket: String, key: String, client: S3Client): S3RangeReader =
    apply(new GetObjectRequest(bucket, key), client)

  /**
   * Returns a new instance of S3RangeReader.
   *
   * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @return A new instance of S3RangeReader.
   */
  def apply(request: GetObjectRequest, client: S3Client): S3RangeReader =
    new S3RangeReader(request, client)
}
