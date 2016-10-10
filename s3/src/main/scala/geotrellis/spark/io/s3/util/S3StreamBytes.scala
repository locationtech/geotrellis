package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._
import org.apache.commons.io.IOUtils

import com.amazonaws.services.s3.model._

class S3StreamBytes(request: GetObjectRequest,
  client: AmazonS3Client,
  chunkSize: Int) extends StreamBytes(chunkSize) {

  def metadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  override val objectLength = metadata.getContentLength
  
  def getArray(start: Int, length: Int): Array[Byte] = ???

  def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk =
      if (!pastLength(length.toInt + start.toInt))
        length
      else
        (objectLength - start).toInt

    client.readRange(start, start + chunk, request)

  }
}

object S3StreamBytes {
  def apply(bucket: String, key: String, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(new GetObjectRequest(bucket, key), client, chunkSize)

  def apply(request: GetObjectRequest, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(request, client, chunkSize)
}
