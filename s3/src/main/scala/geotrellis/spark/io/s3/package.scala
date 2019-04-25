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

package geotrellis.spark.io

import org.apache.spark.rdd.RDD
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{NoSuchKeyException, HeadObjectRequest}

import scala.util.{Try, Success, Failure}
import java.net.URI

package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks
      .collect { case str if str.nonEmpty => if(str.endsWith("/")) str.dropRight(1) else str }
      .mkString("/")

  implicit class withSaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) extends SaveToS3Methods(rdd)

  def s3ObjectExists(bucket: String, key: String, s3Client: S3Client): Boolean = {
    val request = HeadObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    // If successful
    val objectExists =
      Try(s3Client.headObject(request))
        .map(_ => true)
        .recoverWith({ case nske: NoSuchKeyException => Success(false) })
    objectExists match {
      case Success(bool) => bool
      case Failure(throwable) => throw throwable
    }
  }

  def s3ObjectExists(path: String, s3Client: S3Client): Boolean = {
    val arr = path.split("/").filterNot(_.isEmpty)
    val bucket = arr.head
    val key = arr.tail.mkString("/")
    s3ObjectExists(bucket, key, s3Client)
  }
}
