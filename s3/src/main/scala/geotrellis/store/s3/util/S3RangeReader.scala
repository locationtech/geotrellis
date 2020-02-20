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

package geotrellis.store.s3.util

import geotrellis.store.s3._
import geotrellis.util.RangeReader

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import org.apache.commons.io.IOUtils

import java.net.URI

/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff on the
 * AWS S3 server.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @param readHeader: Whether the `HEAD` of the target object should be read or not. Default is `false`.
 * @return A new instance of S3RangeReader.
 */
class S3RangeReader(
  request: => GetObjectRequest,
  client: S3Client,
  readHeader: Boolean = false
) extends RangeReader {

  lazy val totalLength: Long = {
    if (readHeader) {
      val headRequest =
        HeadObjectRequest
          .builder()
          .bucket(request.bucket)
          .key(request.key)
          .build()

      client.headObject(headRequest).contentLength
    } else {
      val responseStream = client.getObject(request)
      val length = responseStream.response.contentLength

      responseStream.abort()
      responseStream.close()
      length
    }
  }

  def readClippedRange(start: Long, length: Int): Array[Byte] = {
    val getRequest =
      GetObjectRequest
        .builder()
        .bucket(request.bucket)
        .key(request.key)
        .range(s"bytes=${start}-${start + length}")
        .build()

    val is = client.getObject(getRequest)
    val bytes = IOUtils.toByteArray(is)
    is.close()
    bytes
  }
}

/** The companion object of [[S3RangeReader]] */
object S3RangeReader {

  def apply(s3address: String, client: S3Client): S3RangeReader =
    apply(new URI(s3address), client)

  // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
  def apply(uri: URI): S3RangeReader =
    apply(uri, S3ClientProducer.get())

  def apply(uri: URI, client: S3Client): S3RangeReader = {
    lazy val s3Uri = new AmazonS3URI(uri)
    lazy val request =
      GetObjectRequest
        .builder()
        .bucket(s3Uri.getBucket())
        .key(s3Uri.getKey())
        .build()

    apply(request, client)
  }

  /**
   * Returns a new instance of S3RangeReader.
   *
   * @param bucket: A string that is the name of the bucket.
   * @param key: A string that is the path to the GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @return A new instance of S3RangeReader.
   */
  def apply(bucket: String, key: String, client: S3Client): S3RangeReader = {
    lazy val request =
      GetObjectRequest
        .builder()
        .bucket(bucket)
        .key(key)
        .build()

    apply(request, client)
  }

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
