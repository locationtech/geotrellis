package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._

class S3ByteBuffer(val request: GetObjectRequest, val client: AmazonS3Client)
  extends S3InputStreamReader with S3Queue {
  private var filePosition = 0
  private var arrayPosition = 0
  private var startingChunk = getMapArray
  private var (offset, arr) = startingChunk.head

  def position = filePosition

  def get: Byte = {
    val value =
      if (arrayPosition < arr.length - 1) {
        arr(arrayPosition)
      } else {
        arrayPosition = 0

        saveChunk(Map(offset -> arr))
        offset = getMapArray.head._1
        arr = getMapArray.head._2

        arr(arrayPosition)
      }
    arrayPosition += 1
    filePosition += 1
    value
  }

  def get(array: Array[Byte], start: Int, end: Int): Array[Byte] = {
    if (isContained(start, end, startingChunk)) {
      System.arraycopy(arr, start, array, 0, end - start)
      arrayPosition = start + end
    } else {
      val mappedArray = getMapArray(start, end)
      val bytes = mappedArray.head._2

      System.arraycopy(bytes, 0, array, 0, end - start)
      saveChunk(mappedArray)
    }
    filePosition = start + end
    array
  }
}

  
object S3ByteBuffer {
  def apply(request: GetObjectRequest, client: AmazonS3Client): S3ByteBuffer =
    new S3ByteBuffer(request, client)

  def apply(bucket: String, key: String, client: AmazonS3Client): S3ByteBuffer =
    new S3ByteBuffer(new GetObjectRequest(bucket, key), client)
}
