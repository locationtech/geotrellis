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

import geotrellis.spark.LayerId
import geotrellis.spark.io._

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

class S3LayerDeleter(val attributeStore: AttributeStore) extends LazyLogging with LayerDeleter[LayerId] {

  def getS3Client: () => S3Client = () =>
    // https://github.com/aws/aws-sdk-java-v2/blob/master/docs/BestPractices.md#reuse-sdk-client-if-possible
    S3Client.create()

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[S3LayerHeader](id)
      val bucket = header.bucket
      val prefix = header.key + "/"
      val s3Client = getS3Client()
      val listRequest = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(prefix)
        .build()

      val iter = s3Client
        .listObjectsV2Paginator(listRequest)
        .contents
        .asScala

      if (iter.size == 0) throw new LayerDeleteError(id)

      iter.foreach { s3obj =>
          val deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(s3obj.key)
            .build()
          s3Client.deleteObject(deleteRequest)
        }

      attributeStore.delete(id)
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
      case e: NoSuchBucketException =>
        logger.info(s"Metadata for $id was not found (no such bucket). Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    }
  }
}

object S3LayerDeleter {
  def apply(attributeStore: AttributeStore): S3LayerDeleter = new S3LayerDeleter(attributeStore)

  def apply(bucket: String, prefix: String): S3LayerDeleter = apply(S3AttributeStore(bucket, prefix))
}
