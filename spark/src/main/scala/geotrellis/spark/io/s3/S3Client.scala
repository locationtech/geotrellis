package geotrellis.spark.io.s3

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider}
import java.io.{InputStream, ByteArrayInputStream, DataInputStream, ByteArrayOutputStream}
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.slf4j._
import scala.collection.JavaConverters._
import scala.util.Random
import com.amazonaws.ClientConfiguration

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

class AmazonS3Client(credentials: AWSCredentials, config: ClientConfiguration) extends S3Client {
  def this(provider: AWSCredentialsProvider) =
    this(provider.getCredentials, new ClientConfiguration())

  def this(provider: AWSCredentialsProvider, config: ClientConfiguration) =
    this(provider.getCredentials, config)

  val s3client = new com.amazonaws.services.s3.AmazonS3Client(credentials, config)

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
