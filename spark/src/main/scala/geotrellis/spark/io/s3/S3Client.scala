package geotrellis.spark.io.s3

import com.amazonaws.auth.AWSCredentialsProvider
import java.io.InputStream
import com.amazonaws.services.s3.model._

trait S3Client {

  def listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing

  def getObject(getObjectRequest: GetObjectRequest): S3Object

  def putObject(putObjectRequest: PutObjectRequest): PutObjectResult

  def getObject(bucketName: String, key: String): S3Object = 
    getObject(new GetObjectRequest(bucketName, key))

  def putObject(bucketName: String, key: String, input: InputStream, metadata: ObjectMetadata): PutObjectResult =
    putObject(new PutObjectRequest(bucketName, key, input, metadata))

  def putObjectWithBackoff(putObjectRequest: PutObjectRequest): PutObjectResult = {
    var ret: PutObjectResult = null
    var backoff = 0
    do {
      try {
        if (backoff > 0) Thread.sleep(backoff)
        ret = putObject(putObjectRequest)
      } catch {
        case e: AmazonS3Exception =>
          backoff = math.max(8, backoff*2)
      }
    } while (ret == null)
    ret
  }
}

class AmazonS3Client(credentialsProvider: AWSCredentialsProvider) extends S3Client {
  val s3client = new com.amazonaws.services.s3.AmazonS3Client(credentialsProvider)

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