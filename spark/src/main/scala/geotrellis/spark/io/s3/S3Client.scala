package geotrellis.spark.io.s3

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider}
import java.io.{InputStream, ByteArrayInputStream, DataInputStream, ByteArrayOutputStream}
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.slf4j._
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

  def putObjectWithBackoff(putObjectRequest: PutObjectRequest): PutObjectResult = {
    var ret: PutObjectResult = null
    val base = 53
    var backoff = 0
    do {
      try {
        if (backoff > 0)
          Thread.sleep(base * Random.nextInt(backoff + 1))
        ret = putObject(putObjectRequest)
      } catch {
        case e: AmazonS3Exception =>
          if (e.getErrorCode == 503 && e.getErrorType == "SlowDown") {
            backoff = math.max(8, backoff*2)
            logger.info(s"Got $e, Backing off for $backoff ms")
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