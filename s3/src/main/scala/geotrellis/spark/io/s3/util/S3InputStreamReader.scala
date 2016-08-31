package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._

import scala.collection.mutable.Queue
import com.amazonaws.services.s3.model._

trait S3InputStreamReader {
  def chunkSize = 256000

  def request: GetObjectRequest
  def client: AmazonS3Client
  
  var location = 0

  private def metadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)
  def objectLength = metadata.getContentLength

  private def pastLength(size: Int): Boolean =
    if (location + size > objectLength) true else false

  private def getStream(start: Int, end: Int): S3ObjectInputStream = {
    val obj = client.readRange(start, end, request)
    obj.getObjectContent
  }

  private def getArray: Array[Byte] =
    getArray(location, chunkSize + location)

  private def getArray(start: Int, length: Int): Array[Byte] = {
    val chunk =
      if (!pastLength(length - start))
        length - start
      else
        (objectLength - (length - start)).toInt

    val arr = Array.ofDim[Byte](chunk)
    val stream = getStream(start, length)

    stream.read(arr, 0, chunk)
    stream.close()

    location = length + start
    arr
  }

  def getMapArray: Map[Long, Array[Byte]] =
    getMapArray(location, chunkSize + location)

  def getMapArray(start: Int, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))

  def isContained(start: Int, end: Int, map: Map[Long, Array[Byte]]): Boolean = {
    val (offset, arr) = map.head
    if (offset >= start && offset + arr.length <= end) true else false
  }
}

trait S3Queue {
  private def mapQueue = Queue[Map[Long, Array[Byte]]]()
  
  def size: Int = mapQueue.length
  
  def saveChunk(chunk: Map[Long, Array[Byte]]): Unit =
    mapQueue.enqueue(chunk)

  def getChunk: Map[Long, Array[Byte]] = mapQueue.dequeue
}
