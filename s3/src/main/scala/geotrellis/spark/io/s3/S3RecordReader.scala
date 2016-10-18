package geotrellis.spark.io.s3

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.scalalogging.LazyLogging
import geotrellis.spark.io.s3.util.S3BytesStreamer
import geotrellis.vector.Extent
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.commons.io.IOUtils

/** This reader will fetch bytes of each key one at a time using [AmazonS3Client.getObject].
  * Subclass must extend [read] method to map from S3 object bytes to (K,V) */
abstract class S3RecordReader[K, V] extends RecordReader[K, V] with LazyLogging {
  var s3client: S3Client = _
  var bucket: String = _
  var keys: Iterator[String] = null
  var curKey: K = _
  var curValue: V = _
  var keyCount: Int = _
  var curCount: Int = 0

  def getS3Client(credentials: AWSCredentials): S3Client =
    new geotrellis.spark.io.s3.AmazonS3Client(credentials, S3Client.defaultConfiguration)

  def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
    val sp = split.asInstanceOf[S3InputSplit]
    s3client = getS3Client(sp.credentials)
    keys = sp.keys.iterator
    keyCount =  sp.keys.length
    bucket = sp.bucket
    logger.debug(s"Initialize split on bucket '$bucket' with $keyCount keys")
  }

  def getProgress: Float = curCount / keyCount

  def read(key: String, obj: S3BytesStreamer): (K, V)

  def read(key: String, e: Extent, obj: S3BytesStreamer): (K, V)

  def read(key: String, e: Option[Extent], obj: S3BytesStreamer): (K, V)

  def read(key: String, obj: Array[Byte]): (K, V)

  def nextKeyValue(): Boolean = {
    if (keys.hasNext){
      val key = keys.next()
      logger.debug(s"Reading: $key")

      val (k, v) = {
        val obj = s3client.getObject(new GetObjectRequest(bucket, key))
        val inStream = obj.getObjectContent
        val objectData = IOUtils.toByteArray(inStream)
        inStream.close()
        read(key, objectData)
      }

      curKey = k
      curValue = v
      curCount += 1
      true
    } else {
      false
    }
  }

  def getCurrentKey: K = curKey

  def getCurrentValue: V = curValue

  def close(): Unit = {}
}
