/*
 * Copyright 2019 Azavea
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
import geotrellis.util.RangeReaderProvider

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI

class S3RangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "s3") true else false
    case null => false
  }

  def rangeReader(uri: URI): S3RangeReader =
    rangeReader(uri, S3ClientProducer.get())

  def rangeReader(uri: URI, s3Client: S3Client): S3RangeReader = {
    val s3Uri = new AmazonS3URI(uri)
    val prefix = Option(s3Uri.getKey()).getOrElse("")

    S3RangeReader(s3Uri.getBucket(), prefix, s3Client)
  }
}
