package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

class S3StreamBytes(
  request: GetObjectRequest,
  client: AmazonS3Client,
  val chunkSize: Int) extends StreamBytes {

  def metadata: ObjectMetadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  def objectLength: Long = metadata.getContentLength
  
  def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk: Long =
      if (!pastLength(length + start))
        length
      else
        objectLength - start

    client.readRange(start, start + chunk, request)
  }
}

object S3StreamBytes {
  def apply(bucket: String, key: String, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(new GetObjectRequest(bucket, key), client, chunkSize)

  def apply(request: GetObjectRequest, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(request, client, chunkSize)
}
