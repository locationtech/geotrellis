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

package geotrellis.spark.io.s3

import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.{ByteReader, StreamingByteReader}

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI

package object cog {
  def s3PathExists(str: String, s3Client: S3Client): Boolean = {
    val arr = str.split("/").filterNot(_.isEmpty)
    val bucket = arr.head
    val prefix = arr.tail.mkString("/")
    s3Client.doesObjectExist(bucket, prefix)
  }

  def byteReader(uri: URI, s3Client: S3Client): ByteReader = {
    //val auri = new AmazonS3URI(uri)
    val bucket = ???
    val key = ???

    StreamingByteReader(
      S3RangeReader(
        bucket = bucket,
        key    = key,
        client = s3Client
      )
    )
  }
}
