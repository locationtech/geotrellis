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

package geotrellis.spark.io.s3

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext, RecordReader}
import org.apache.commons.io.IOUtils

/** This reader will fetch bytes of each key one at a time using [AmazonS3Client.getObject].
  * Subclass must extend [read] method to map from S3 object bytes to (K,V) */
abstract class S3RecordReader[K, V] extends RecordReader[K, V] with LazyLogging {
  var s3client: S3Client = _
  var bucket: String = _
  var keys: Iterator[String] = null
  var curKey: K = _
  var curValue: V = _
  var keyCount: Int = _
  var curCount: Int = 0

  def getS3Client(credentials: AWSCredentials): S3Client =
    new geotrellis.spark.io.s3.AmazonS3Client(credentials, S3Client.defaultConfiguration)

  def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
    val sp = split.asInstanceOf[S3InputSplit]
    s3client = getS3Client(sp.credentials)
    keys = sp.keys.iterator
    keyCount =  sp.keys.length
    bucket = sp.bucket
    logger.debug(s"Initialize split on bucket '$bucket' with $keyCount keys")
  }

  def getProgress: Float = curCount / keyCount

  def read(key: String, obj: Array[Byte]): (K, V)

  def nextKeyValue(): Boolean = {
    if (keys.hasNext){
      val key = keys.next()
      logger.debug(s"Reading: $key")
      val obj = s3client.getObject(new GetObjectRequest(bucket, key))
      val inStream = obj.getObjectContent
      val objectData = IOUtils.toByteArray(inStream)
      inStream.close()

      val (k, v) = read(key, objectData)
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
