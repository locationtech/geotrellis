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

import geotrellis.store.s3.util.S3RangeReader
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.S3Client
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext, RecordReader}
import org.apache.commons.io.IOUtils

/** This is the base class for readers that will create key value pairs for object requests.
  * Subclass must extend [readObjectRequest] method to map from S3 object requests to (K,V) */
abstract class BaseS3RecordReader[K, V](s3Client: S3Client) extends RecordReader[K, V] with LazyLogging {
  protected var bucket: String = _
  protected var keys: Iterator[String] = null
  protected var curKey: K = _
  protected var curValue: V = _
  protected var keyCount: Int = _
  protected var curCount: Int = 0

  def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
    val sp = split.asInstanceOf[S3InputSplit]
    keys = sp.keys.iterator
    keyCount =  sp.keys.length
    bucket = sp.bucket
    logger.debug(s"Initialize split on bucket '$bucket' with $keyCount keys")
  }

  def getProgress: Float = curCount / keyCount

  def readObjectRequest(objectRequest: GetObjectRequest): (K, V)

  def nextKeyValue(): Boolean = {
    if (keys.hasNext){
      val key = keys.next()
      val request = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val (k, v) = readObjectRequest(request)

      curKey = k
      curValue = v
      curCount += 1
      true
    } else {
      false
    }
  }

  def getCurrentKey: K = curKey

  def getCurrentValue: V = curValue

  def close(): Unit = {}
}

/** This reader will fetch bytes of each key one at a time using [AmazonS3Client.getObject].
  * Subclass must extend [read] method to map from S3 object bytes to (K,V) */
abstract class S3RecordReader[K, V](s3Client: S3Client) extends BaseS3RecordReader[K, V](s3Client: S3Client) {
  def readObjectRequest(objectRequest: GetObjectRequest): (K, V) = {
    val response = s3Client.getObject(objectRequest)
    val objectData = IOUtils.toByteArray(response)
    response.close()

    read(objectRequest.key, objectData)
  }

  def read(key: String, obj: Array[Byte]): (K, V)
}

/** This reader will stream bytes of each key one at a time using [AmazonS3Client.getObject].
  * Subclass must extend [read] method to map from S3RangeReader to (K,V) */
abstract class StreamingS3RecordReader[K, V](s3Client: S3Client) extends BaseS3RecordReader[K, V](s3Client: S3Client) {
  def readObjectRequest(objectRequest: GetObjectRequest): (K, V) = {
    val byteReader =
      StreamingByteReader(S3RangeReader(objectRequest, s3Client))

    read(objectRequest.key, byteReader)
  }

  def read(key: String, byteReader: ByteReader): (K, V)
}
