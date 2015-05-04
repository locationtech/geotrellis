package geotrellis.spark.io.s3

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider}
import java.io.{InputStream, ByteArrayInputStream, DataInputStream, ByteArrayOutputStream}
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.slf4j._
import scala.collection.JavaConverters._
import scala.util.Random

trait S3Client extends LazyLogging {

  def listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing
  
  def listObjects(bucketName: String, prefix: String): ObjectListing =          
      listObjects(new ListObjectsRequest(bucketName, prefix, null, null, null));

  def getObject(getObjectRequest: GetObjectRequest): S3Object

  def putObject(putObjectRequest: PutObjectRequest): PutObjectResult

  def getObject(bucketName: String, key: String): S3Object = 
    getObject(new GetObjectRequest(bucketName, key))

  def putObject(bucketName: String, key: String, input: InputStream, metadata: ObjectMetadata): PutObjectResult = 
    putObject(new PutObjectRequest(bucketName, key, input, metadata))

  def putObject(bucketName: String, key: String, bytes: Array[Byte], metadata: ObjectMetadata): PutObjectResult = {    
    metadata.setContentLength(bytes.length)
    putObject(bucketName, key, new ByteArrayInputStream(bytes), metadata)
  }

  def putObject(bucketName: String, key: String, bytes: Array[Byte]): PutObjectResult =
    putObject(bucketName, key, bytes, new ObjectMetadata())


  def listObjectsIterator(bucketName: String, prefix: String, maxKeys: Int = 0): Iterator[S3ObjectSummary] =          
      listObjectsIterator(new ListObjectsRequest(bucketName, prefix, null, null, if (maxKeys == 0) null else maxKeys));

  def listObjectsIterator(request: ListObjectsRequest): Iterator[S3ObjectSummary] =
    new Iterator[S3ObjectSummary] {      
      var listing = listObjects(request)
      var iter = listing.getObjectSummaries.asScala.iterator

      def getNextPage: Boolean =  {
        val nextRequest = request.withMarker(listing.getNextMarker)
        listing = listObjects(nextRequest)
        listing.getObjectSummaries.asScala.iterator        
        iter.hasNext
      }

      def hasNext: Boolean = {
        iter.hasNext || getNextPage
      }      

      def next: S3ObjectSummary = iter.next
    }          
      

  def putObjectWithBackoff(putObjectRequest: PutObjectRequest): PutObjectResult = {
    var ret: PutObjectResult = null
    val base = 53
    var backoff = 0
    do {
      if (backoff > 0){
        val pause = base * Random.nextInt(math.pow(2,backoff).toInt) // .extInt is [), implying -1
        logger.info("Backing off for $pause ms")
        Thread.sleep(pause) 
      }

      try {
        ret = putObject(putObjectRequest)
      } catch {
        case e: AmazonS3Exception =>
          if (e.getStatusCode == 503) {
            backoff = +1
          }else if (e.getStatusCode == 400) {
            backoff = 0 // socket timeout, just retry
          }else{
            throw e
          }
      }
    } while (ret == null)
    ret
  }
}

object S3Client {
  implicit class S3ObjectBytes(obj: S3Object) {
    def toBytes: Array[Byte] = {
      val is = obj.getObjectContent
      val len = obj.getObjectMetadata.getContentLength.toInt
      val bytes = new Array[Byte](len)
      val read = is.read(bytes, 0, len)
      is.close()
      assert(read == len, s"$read bytes read, $len expected.")      
      bytes
    }
  }
}

class AmazonS3Client(credentials: AWSCredentials) extends S3Client {
  def this(provider: AWSCredentialsProvider) =
    this(provider.getCredentials)

  val s3client = new com.amazonaws.services.s3.AmazonS3Client(credentials)

  def listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing = {
    s3client.listObjects(listObjectsRequest)
  }

  def getObject(getObjectRequest: GetObjectRequest): S3Object = {
    s3client.getObject(getObjectRequest)
  }

  def putObject(putObjectRequest: PutObjectRequest): PutObjectResult = {
    s3client.putObject(putObjectRequest)
  }
}