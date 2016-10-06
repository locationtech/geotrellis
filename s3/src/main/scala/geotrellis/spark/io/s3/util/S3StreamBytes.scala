package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

trait S3StreamBytes extends StreamBytes {
  def client: AmazonS3Client
  def request: GetObjectRequest

  def metadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  def objectLength = metadata.getContentLength

  def readStream(start: Int, end: Int): S3ObjectInputStream = {
    //println(s"reading the stream now. Start: $start, End: $end")
    val obj = client.readRange(start.toLong, end.toLong, request)
    val s = obj.getObjectContent
    s
  }
}
