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

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.auth._
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest.KeyVersion
import software.amazon.awssdk.retry.PredefinedRetryPolicies
import software.amazon.awssdk.services.s3.model._

import java.io.{InputStream, ByteArrayInputStream}
import scala.annotation.tailrec
import scala.collection.JavaConverters._

// TODO: BURN this
trait dS3Client extends LazyLogging with Serializable {

  def doesBucketExist(bucket: String): Boolean

  def doesObjectExist(bucket: String, key: String): Boolean

  def listObjects(listObjectsRequest: ListObjectsRequest): ListObjectsResponse

  def listObjects(bucketName: String, prefix: String): ListObjectsResponse = {
    val request = ListObjectsRequest.builder()
      .bucket(bucketName)
      .key(prefix)
      .maxKeys(maxKeys)
      .build()
    listObjects(request)
  }

  def listKeys(bucketName: String, prefix: String): Seq[String] =
    listKeys(new ListObjectsRequest(bucketName, prefix, null, null, null))

  def listKeys(listObjectsRequest: ListObjectsRequest): Seq[String]

  def getObject(getObjectRequest: GetObjectRequest): ResponseInputStream[GetObjectResponse]

  def putObject(putObjectRequest: PutObjectRequest, requestBody: RequestBody): PutObjectResponse

  def listNextBatchOfObjects(listing: ObjectListing): ObjectListing

  @tailrec
  final def deleteListing(bucket: String, listing: ObjectListing): Unit = {
    val listings = listing
      .getObjectSummaries
      .asScala
      .map { os => new KeyVersion(os.getKey) }
      .toList

    // Empty listings cause malformed XML to be sent to AWS and lead to unhelpful exceptions
    if (listings.nonEmpty) {
      deleteObjects(bucket, listings)
      if (listing.isTruncated) deleteListing(bucket, listNextBatchOfObjects(listing))
    }
  }

  def deleteObject(deleteObjectRequest: DeleteObjectRequest): Unit

  def copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult

  def deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): Unit

  def getObject(bucketName: String, key: String): ResponseInputStream[GetObjectResponse] = {
    val request = GetObjectResponse.builder()
      .bucket(bucketName)
      .key(key)
      .build()
    getObject(request)
  }

  def deleteObjects(bucketName: String, keys: List[KeyVersion]): Unit = {
    val objectsDeleteRequest = new DeleteObjectsRequest(bucketName)
    objectsDeleteRequest.setKeys(keys.asJava)
    deleteObjects(objectsDeleteRequest)
  }

  def copyObject(sourceBucketName: String, sourceKey: String, destinationBucketName: String, destinationKey: String): CopyObjectResult =
    copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey))

  def deleteObject(bucketName: String, key: String): Unit =
    deleteObject(new DeleteObjectRequest(bucketName, key))

  def putObject(bucketName: String, key: String, input: InputStream, metadata: ObjectMetadata): PutObjectResult =
    putObject(new PutObjectRequest(bucketName, key, input, metadata))

  def putObject(bucketName: String, key: String, bytes: Array[Byte], metadata: ObjectMetadata): PutObjectResult = {
    metadata.setContentLength(bytes.length)
    putObject(bucketName, key, new ByteArrayInputStream(bytes), metadata)
  }

  def putObject(bucketName: String, key: String, bytes: Array[Byte]): PutObjectResult =
    putObject(bucketName, key, bytes, new ObjectMetadata())

  def readBytes(bucketName: String, key: String): Array[Byte] =
    readBytes(new GetObjectRequest(bucketName, key))

  def readBytes(getObjectRequest: GetObjectRequest): Array[Byte]

  def readRange(start: Long, end: Long, getObjectRequest: GetObjectRequest): Array[Byte]

  def getObjectMetadata(bucketName: String, key: String): ObjectMetadata =
    getObjectMetadata(new GetObjectMetadataRequest(bucketName, key))

  def getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata

  def listObjectsV2Paginator(bucketName: String, prefix: String, maxKeys: Int = 0): Iterator[S3Object] = {
    val request = ListObjectsRequest.builder()
      .bucket(bucketName)
      .key(prefix)
      .maxKeys(maxKeys)
      .build()
    listObjectsV2Paginator(request)
  }

  def listObjectsV2Paginator(listObjectsRequest: ListObjectsV2Request): Iterator[S3Object] =
    listObjectsV2Paginator(listObjectsRequest)
      .contents()
      .asScala()

  def listObjectsV2Paginator(listObjectsRequest: ListObjectsV2Request): ListObjectsV2Iterable

  def listObjectsIterator(bucketName: String, prefix: String, maxKeys: Int = 0): Iterator[S3ObjectSummary] =
    listObjectsIterator(new ListObjectsRequest(bucketName, prefix, null, null, if (maxKeys == 0) null else maxKeys))

  def listObjectsIterator(request: ListObjectsRequest): Iterator[S3ObjectSummary] =
    new Iterator[S3ObjectSummary] {
      var listing = listObjects(request)
      var iter = listing.getObjectSummaries.asScala.iterator

      def getNextPage: Boolean =  {
        listing.isTruncated && {
          val nextRequest = request.withMarker(listing.getNextMarker)
          listing = listObjects(nextRequest)
          iter = listing.getObjectSummaries.asScala.iterator
          iter.hasNext
        }
      }

      def hasNext: Boolean = {
        iter.hasNext || getNextPage
      }

      def next: S3ObjectSummary = iter.next
    }

  def setRegion(region: software.amazon.awssdk.regions.Region): Unit

  def utilities: S3Utilities
}

object WrappedS3Client {
  def defaultConfiguration = {
    val config = new software.amazon.awssdk.ClientConfiguration
    config.setMaxConnections(128)
    config.setMaxErrorRetry(16)
    config.setConnectionTimeout(100000)
    config.setSocketTimeout(100000)
    config.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(32))
    config
  }

  def DEFAULT =
    AmazonS3Client(DefaultAWSCredentialsProviderChain.getInstance(), defaultConfiguration)

  def ANONYMOUS =
    AmazonS3Client(new AnonymousAWSCredentials(), defaultConfiguration)
}
