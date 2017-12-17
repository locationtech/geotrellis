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

package geotrellis.spark.io.s3.testkit

import geotrellis.spark.io.s3._
import geotrellis.util.LazyLogging
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import scala.collection.mutable

class MockS3Client() extends S3Client with LazyLogging {
  import MockS3Client._

  def doesBucketExist(bucket: String): Boolean = buckets.containsKey(bucket)

  def doesObjectExist(bucket: String, key: String): Boolean =
    getBucket(bucket).contains(key)

  def listObjects(r: ListObjectsRequest): ObjectListing = this.synchronized {
    setLastListObjectsRequest(r)
    if (null == r.getMaxKeys)
      r.setMaxKeys(64)

    val ol = new ObjectListing
    ol.setBucketName(r.getBucketName)
    ol.setPrefix(r.getPrefix)
    ol.setDelimiter(r.getDelimiter)
    ol.setMaxKeys(r.getMaxKeys)
    val listing = ol.getObjectSummaries


    val bucket = getBucket(r.getBucketName)
    var marker = r.getMarker

    if (null == marker) {
      bucket.findFirstKey(r.getPrefix) match {
        case Some(key) => marker = key
        case None => return ol
      }
      logger.debug(s"MOVING MARKER prefix=${r.getPrefix} marker=${marker}")
    }

    var keyCount = 0
    var nextMarker: String = null
    val iter = bucket.entries.from(marker).iterator
    logger.debug(s"LISTING prefix=${r.getPrefix}, marker=$marker")

    var endSeen = false
    while (iter.hasNext && keyCount <= r.getMaxKeys) {
      val (key, bytes) = iter.next
      if (key startsWith r.getPrefix) {
        if (keyCount < r.getMaxKeys){
          logger.debug(s" + ${key}")
          val os = new S3ObjectSummary
          os.setBucketName(bucket.name)
          os.setKey(key)
          os.setSize(bytes.length)
          listing.add(os)
        }
        if (keyCount == r.getMaxKeys) {
          nextMarker = key
        }
        keyCount += 1
      }else{
        endSeen = true
      }
    }

    ol.setNextMarker(nextMarker)
    ol.setTruncated(null != nextMarker)
    ol
  }

  def getObject(r: GetObjectRequest): S3Object =  this.synchronized {
    val bucket = getBucket(r.getBucketName)
    val key = r.getKey
    logger.debug(s"GET ${r.getKey}")
    bucket.synchronized {
      if (bucket.contains(key)) {
        val obj = new S3Object()
        val bytes = bucket(key)
        val md = new ObjectMetadata()
        md.setContentLength(bytes.length)
        obj.setKey(key)
        obj.setBucketName(r.getBucketName)
        obj.setObjectContent(new ByteArrayInputStream(bytes))
        obj.setObjectMetadata(md)
        obj
      } else {
        val ex = new AmazonS3ExceptionBuilder()
        ex.setErrorCode("NoSuchKey")
        ex.setErrorMessage(s"The specified key does not exist: $key")
        ex.setStatusCode(404)
        throw ex.build
      }
    }
  }

  def listKeys(listObjectsRequest: ListObjectsRequest): Seq[String] = {
    var listing: ObjectListing = null
    val result = mutable.ListBuffer[String]()
    do {
      listing = listObjects(listObjectsRequest)
      // avoid including "directories" in the input split, can cause 403 errors on GET
      result ++= listing.getObjectSummaries.asScala.map(_.getKey).filterNot(_ endsWith "/")
      listObjectsRequest.setMarker(listing.getNextMarker)
    } while (listing.isTruncated)

    result
  }

  def readBytes(getObjectRequest: GetObjectRequest): Array[Byte] = {
    val obj = getObject(getObjectRequest)
    val inStream = obj.getObjectContent
    try {
      IOUtils.toByteArray(inStream)
    } finally {
      inStream.close()
    }
  }

  def readRange(start: Long, end: Long, r: GetObjectRequest): Array[Byte] = {
    r.setRange(start, end)
    val obj = getObject(r)
    val stream = obj.getObjectContent
    try {
      val diff = (end - start).toInt
      val arr = Array.ofDim[Byte](diff)
      stream.skip(start)
      stream.read(arr, 0, arr.size)
      arr
    } finally {
      stream.close()
    }
  }

  def putObject(r: PutObjectRequest): PutObjectResult = this.synchronized {
    logger.debug(s"PUT ${r.getKey}")
    val bucket = getBucket(r.getBucketName)
    bucket.synchronized {
      bucket.put(r.getKey, IOUtils.toByteArray(r.getInputStream))
    }
    new PutObjectResult()
  }

  def deleteObject(r: DeleteObjectRequest): Unit = this.synchronized {
    logger.debug(s"DELETE ${r.getKey}")
    val bucket = getBucket(r.getBucketName)
    bucket.synchronized {
      bucket.remove(r.getKey)
    }
  }

  def listNextBatchOfObjects(r: ObjectListing): ObjectListing = this.synchronized {
    if(!r.isTruncated) r
    else {
      val ol = new ObjectListing
      ol.setBucketName(r.getBucketName)
      ol.setPrefix(r.getPrefix)
      ol.setDelimiter(r.getDelimiter)
      ol.setMaxKeys(r.getMaxKeys)
      val listing = ol.getObjectSummaries

      val bucket = getBucket(r.getBucketName)
      val marker = r.getNextMarker

      var keyCount = 0
      var nextMarker: String = null
      val iter = bucket.entries.from(marker).iterator
      logger.debug(s"LISTING prefix=${r.getPrefix}, marker=$marker")

      var endSeen = false
      while (iter.hasNext && keyCount <= r.getMaxKeys) {
        val (key, bytes) = iter.next
        if (key startsWith r.getPrefix) {
          if (keyCount < r.getMaxKeys) {
            logger.debug(s" + ${key}")
            val os = new S3ObjectSummary
            os.setBucketName(bucket.name)
            os.setKey(key)
            os.setSize(bytes.length)
            listing.add(os)
          }
          if (keyCount == r.getMaxKeys) {
            nextMarker = key
          }
          keyCount += 1
        } else {
          endSeen = true
        }
      }

      ol.setNextMarker(nextMarker)
      ol.setTruncated(null != nextMarker)
      ol
    }
  }

  def deleteObjects(r: DeleteObjectsRequest): Unit = {
    val keys = r.getKeys
    logger.debug(s"DELETE LIST ${keys}")

    val bucket = getBucket(r.getBucketName)
    bucket.synchronized {
      bucket.remove(keys.asScala.map(_.getKey))
    }
  }

  def copyObject(r: CopyObjectRequest): CopyObjectResult = this.synchronized {
    logger.debug(s"COPY ${r.getSourceKey}")

    val destBucket = getBucket(r.getDestinationBucketName)
    destBucket.synchronized {
      val obj = getObject(r.getSourceBucketName, r.getSourceKey)
      putObject(r.getDestinationBucketName, r.getDestinationKey, obj.getObjectContent, obj.getObjectMetadata)
    }
    new CopyObjectResult()
  }

  def setRegion(region: com.amazonaws.regions.Region): Unit = {}

  def getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata = {
    val (b, k) = (getObjectMetadataRequest.getBucketName, getObjectMetadataRequest.getKey)
    getObject(new GetObjectRequest(b, k)).getObjectMetadata
  }
}

object MockS3Client{
  class Bucket(val name: String) {
    var entries = new TreeMap[String, Array[Byte]]

    def apply(key: String): Array[Byte] =
      entries.get(key).get

    def put(key: String, bytes: Array[Byte]) =
      entries += key -> bytes

    def contains(key: String): Boolean =
      entries.contains(key)

    def remove(key: String) =
      entries -= key

    def remove(keys: Seq[String]) =
      entries --= keys

    /** return first key that matches this prefix */
    def findFirstKey(prefix: String): Option[String] = {
      entries
        .find { case (key, bytes) => key startsWith prefix }
        .map { _._1 }
    }
  }

  def reset(): Unit = {
    buckets.clear()
    _lastListObjectsRequest = None
  }

  val buckets = new ConcurrentHashMap[String, Bucket]()

  def getBucket(name: String): Bucket = {
    if (buckets.containsKey(name)) {
      buckets.get(name)
    }else{
      val bucket = new Bucket(name)
      buckets.put(name, bucket)
      bucket
    }
  }

  // Allow tests to inspect the last ListObjectRequest

  var _lastListObjectsRequest: Option[ListObjectsRequest] = None
  def lastListObjectsRequest = _lastListObjectsRequest
  def setLastListObjectsRequest(r: ListObjectsRequest) =
    _lastListObjectsRequest.synchronized {
      _lastListObjectsRequest = Some(r)
    }
}
