package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._

import java.nio.ByteBuffer
import com.amazonaws.services.s3.model._

class S3ByteBuffer(request: GetObjectRequest, client: AmazonS3Client) {
  var position = 0

  val metadata = client.getObjectMetadata(request.getBucketName, request.getKey)
  val objectLength = metadata.getContentLength

  def getStream(end: Int): S3ObjectInputStream = {
    val obj = client.readRange(position, end, request)
    obj.getObjectContent
  }

  def getArray(chunk: Int): Array[Byte] = {
    val arr = Array.ofDim[Byte](chunk)
    val stream = getStream(chunk + position)
    stream.read(arr, 0, chunk)
    position += chunk
    arr
  }

  def extendArray(chunk: Int, arr: Array[Byte]): Array[Byte] = {
    val temp = getArray(chunk)
    val newArray = Array.ofDim[Byte](position)
    System.arraycopy(arr, 0, newArray, 0, arr.length)
    System.arraycopy(temp, 0, newArray, arr.length, temp.length)

    newArray
  }
}

object S3ByteBuffer {
  def apply(request: GetObjectRequest, client: AmazonS3Client): S3ByteBuffer =
    new S3ByteBuffer(request, client)

  def apply(bucket: String, key: String, client: AmazonS3Client): S3ByteBuffer =
    new S3ByteBuffer(new GetObjectRequest(bucket, key), client)
}
