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

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth._
import com.amazonaws.services.s3.{AmazonS3Client => AWSAmazonS3Client}
import com.amazonaws.services.s3.model._
import sun.misc.IOUtils

import scala.collection.JavaConverters._
import scala.collection.mutable

object AmazonS3Client {
  def apply(s3client: AWSAmazonS3Client): AmazonS3Client =
    new AmazonS3Client(s3client)

  def apply(credentials: AWSCredentials, config: ClientConfiguration): AmazonS3Client =
    apply(new AWSAmazonS3Client(credentials, config))

  def apply(provider: AWSCredentialsProvider, config: ClientConfiguration): AmazonS3Client =
    apply(new AWSAmazonS3Client(provider, config))

  def apply(provider: AWSCredentialsProvider): AmazonS3Client =
    apply(provider, new ClientConfiguration())

}

class AmazonS3Client(s3client: AWSAmazonS3Client) extends S3Client {
  def doesBucketExist(bucket: String): Boolean =
    s3client.doesBucketExist(bucket)

  def doesObjectExist(bucket: String, key: String): Boolean =
    s3client.doesObjectExist(bucket, key)

  def listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing =
    s3client.listObjects(listObjectsRequest)

  def listKeys(listObjectsRequest: ListObjectsRequest): Seq[String] = {
    var listing: ObjectListing = null
    val result = mutable.ListBuffer[String]()
    do {
      listing = s3client.listObjects(listObjectsRequest)
      // avoid including "directories" in the input split, can cause 403 errors on GET
      result ++= listing.getObjectSummaries.asScala.map(_.getKey).filterNot(_ endsWith "/")
      listObjectsRequest.setMarker(listing.getNextMarker)
    } while (listing.isTruncated)

    result
  }

  def getObject(getObjectRequest: GetObjectRequest): S3Object =
    s3client.getObject(getObjectRequest)

  def putObject(putObjectRequest: PutObjectRequest): PutObjectResult =
    s3client.putObject(putObjectRequest)

  def deleteObject(deleteObjectRequest: DeleteObjectRequest): Unit =
    s3client.deleteObject(deleteObjectRequest)

  def copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult =
    s3client.copyObject(copyObjectRequest)

  def listNextBatchOfObjects(listing: ObjectListing): ObjectListing =
    s3client.listNextBatchOfObjects(listing)

  def deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): Unit =
    s3client.deleteObjects(deleteObjectsRequest)

  def readBytes(getObjectRequest: GetObjectRequest): Array[Byte] = {
    val obj = s3client.getObject(getObjectRequest)
    val inStream = obj.getObjectContent
    try {
      IOUtils.readFully(inStream, -1, true)
    } finally {
      inStream.close()
    }
  }

  def readRange(start: Long, end: Long, getObjectRequest: GetObjectRequest): Array[Byte] = {
    getObjectRequest.setRange(start, end - 1)
    val obj = s3client.getObject(getObjectRequest)
    val stream = obj.getObjectContent
    try {
      IOUtils.readFully(stream, -1, true)
    } finally {
      stream.close()
    }
  }

  def getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata =
    s3client.getObjectMetadata(getObjectMetadataRequest)

  def setRegion(region: com.amazonaws.regions.Region): Unit = {
    s3client.setRegion(region)
  }
}
