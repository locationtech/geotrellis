package geotrellis.spark.io.s3

import com.amazonaws.auth.AWSCredentialsProvider
import java.io.{InputStream, ByteArrayInputStream}
import com.amazonaws.services.s3.model._
import java.util.concurrent.ConcurrentHashMap
import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder
import scala.collection.immutable.TreeMap
import com.typesafe.scalalogging.slf4j._

class MockS3Client(credentialsProvider: AWSCredentialsProvider) extends S3Client with LazyLogging {
  import MockS3Client._

  def listObjects(r: ListObjectsRequest): ObjectListing = {
    if (null == r.getMaxKeys)
      r.setMaxKeys(64)
    val bucket = getBucket(r.getBucketName)
    var marker = r.getMarker
    
    if (null == marker) {
      marker = bucket.findFirstKey(r.getPrefix).get
      logger.debug(s"MOVING MARKER prefix=${r.getPrefix} marker=${marker}")
    }

    var keyCount = 0
    var nextMarker: String = null
    val iter = bucket.entries.from(marker).iterator
    logger.debug(s"LISTING prefix=${r.getPrefix}, marker=$marker")
    
    val ol = new ObjectListing
    ol.setBucketName(r.getBucketName)
    ol.setPrefix(r.getPrefix)
    ol.setDelimiter(r.getDelimiter)
    ol.setMaxKeys(r.getMaxKeys)
    val listing = ol.getObjectSummaries
    
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

  def getObject(r: GetObjectRequest): S3Object = {
    val bucket = getBucket(r.getBucketName)
    val key = r.getKey
    if (bucket.contains(key)){
      val obj = new S3Object()
      val bytes = bucket(key)      
      val md = new ObjectMetadata()
      md.setContentLength(bytes.length)            
      obj.setKey(key)      
      obj.setBucketName(r.getBucketName)
      obj.setObjectContent(new ByteArrayInputStream(bytes))      
      obj.setObjectMetadata(md)
      obj
    }else{
      val ex = new AmazonS3ExceptionBuilder()
      ex.setErrorCode("NoSuchKey")
      ex.setErrorMessage("The specified key does not exist")
      ex.setStatusCode(404)      
      throw ex.build
    }
  }

  def putObject(r: PutObjectRequest): PutObjectResult = {    
    logger.info(s"PUT ${r.getKey}")
    val bucket = getBucket(r.getBucketName)
    bucket.put(r.getKey, streamToBytes(r.getInputStream))
    new PutObjectResult()
  }
}

object MockS3Client{
  class Bucket(val name: String) {
    var entries = new TreeMap[String, Array[Byte]]

    def apply(key: String): Array[Byte] = {
      entries.get(key).get
    }

    def put(key: String, bytes: Array[Byte]) = {
      entries += key -> bytes
    }

    def contains(key: String): Boolean =
      entries.contains(key)

    /** return first key that matches this prefix */
    def findFirstKey(prefix: String): Option[String] = {
      entries
        .find{ case (key, bytes) => key startsWith prefix }
        .map { _._1 }
    }
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

  def streamToBytes(is: InputStream) =
    S3RecordReader.readInputStream(is)
}

