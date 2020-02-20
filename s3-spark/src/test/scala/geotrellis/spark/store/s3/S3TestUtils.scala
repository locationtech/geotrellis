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

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import scala.collection.JavaConverters._
import scala.collection.mutable

object S3TestUtils {
  def cleanBucket(client: S3Client, bucket: String) = {
    try {
      val listObjectsReq =
        ListObjectsV2Request.builder()
          .bucket(bucket)
          .build()
      val objIdentifiers = client.listObjectsV2Paginator(listObjectsReq)
        .contents
        .asScala
        .map { s3obj => ObjectIdentifier.builder.key(s3obj.key).build() }
        .toList
      val deleteDefinition = Delete.builder()
        .objects(objIdentifiers:_*)
        .build()
      val deleteReq = DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(deleteDefinition)
        .build()
      client.deleteObjects(deleteReq)
    } catch {
      case nsb: NoSuchBucketException =>
        val createBucketReq =
          CreateBucketRequest.builder()
            .bucket(bucket)
            .build()
        client.createBucket(createBucketReq)
    }
  }
}
