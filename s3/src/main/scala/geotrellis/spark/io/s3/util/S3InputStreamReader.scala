package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._

import scala.collection.mutable.Queue
import com.amazonaws.services.s3.model._
import java.nio.ByteBuffer

trait S3Bytes {
  def client: AmazonS3Client
  def request: GetObjectRequest

  private val chunkSize = 256000
  private var streamPosition = 0

  private val metadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  private val objectLength = metadata.getContentLength

  private def pastLength(size: Int): Boolean =
    if (size > objectLength) true else false

  private def readStream(start: Int, end: Int): S3ObjectInputStream = {
    val obj = client.readRange(start, end, request)
    obj.getObjectContent
  }

  private def getArray: Array[Byte] =
    getArray(streamPosition)

  private def getArray(start: Int): Array[Byte] =
    getArray(start, chunkSize)

  private def getArray(start: Int, length: Int): Array[Byte] = {
    val chunk =
      if (!pastLength(length + start))
        length
      else
        (objectLength - start).toInt

    val arr = Array.ofDim[Byte](chunk)
    val stream = readStream(start, chunk)

    stream.read(arr, 0, chunk)
    streamPosition = start + length

    arr
  }

  def getMappedArray: Map[Long, Array[Byte]] =
    getMappedArray(streamPosition, chunkSize)

  def getMappedArray(start: Int): Map[Long, Array[Byte]] =
    getMappedArray(start, chunkSize)

  def getMappedArray(start: Int, length: Int): Map[Long, Array[Byte]] =
    Map(start.toLong -> getArray(start, length))
}

object S3Queue {
  private val mapQueue = Queue[Map[Long, Array[Byte]]]()
  
  def size: Int = mapQueue.length

  def isEmpty: Boolean = mapQueue.isEmpty
  
  def saveChunk(chunk: Map[Long, Array[Byte]]): Unit =
    mapQueue.enqueue(chunk)

  def getChunk: Map[Long, Array[Byte]] = mapQueue.dequeue
}
