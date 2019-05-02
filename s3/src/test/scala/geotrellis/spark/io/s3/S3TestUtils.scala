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

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import scala.collection.JavaConverters._

object S3TestUtils {
  def createBucket(client: S3Client, name: String) = {
    val createBucketReq =
      CreateBucketRequest.builder()
        .bucket(name)
        .build()
    client.createBucket(createBucketReq)
  }

  def cleanBucket(client: S3Client, bucket: String) = {
    val listObjectsReq =
      ListObjectsV2Request.builder()
        .bucket(bucket)
        .build()
    client.listObjectsV2Paginator(listObjectsReq)
      .contents
      .asScala
      .foreach { s3obj =>
        val deleteReq = DeleteObjectRequest.builder().bucket(bucket).key(s3obj.key).build()
        client.deleteObject(deleteReq)
      }
    val deleteBucketReq =
      DeleteBucketRequest.builder()
        .bucket(bucket)
        .build()
    client.deleteBucket(deleteBucketReq)
    createBucket(client, bucket)
  }
}
