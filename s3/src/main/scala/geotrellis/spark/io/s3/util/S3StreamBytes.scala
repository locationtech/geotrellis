package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._

import scala.collection.mutable.Queue
import com.amazonaws.services.s3.model._

trait S3StreamBytes extends StreamBytes {
  def client: AmazonS3Client
  def request: GetObjectRequest

  def metadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  def objectLength = metadata.getContentLength

  def readStream(start: Int, end: Int): S3ObjectInputStream = {
    val obj = client.readRange(start, end, request)
    obj.getObjectContent
  }
}


object S3Queue {
  private val mapQueue = Queue[Map[Long, Array[Byte]]]()
  
  def size: Int = mapQueue.length

  def isEmpty: Boolean = mapQueue.isEmpty
  
  def saveChunk(chunk: Map[Long, Array[Byte]]): Unit =
    mapQueue.enqueue(chunk)

  def getChunk: Map[Long, Array[Byte]] = mapQueue.dequeue
}
