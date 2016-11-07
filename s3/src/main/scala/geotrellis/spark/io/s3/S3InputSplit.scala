package geotrellis.spark.io.s3

import java.io.{DataOutput, DataInput}
import com.amazonaws.services.s3.model.{S3ObjectSummary, ObjectListing}
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce.InputSplit
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials, AnonymousAWSCredentials, BasicSessionCredentials}
import com.typesafe.scalalogging.LazyLogging

/**
 * Represents are batch of keys to be read from an S3 bucket.
 * AWS credentials have already been discovered and provided by the S3InputFormat.
 */
class S3InputSplit extends InputSplit with Writable with LazyLogging
{
  var sessionToken: String = null
  var bucket: String = _
  var keys: Seq[String] = Vector.empty
  /** Combined size of objects in bytes */
  var size: Long = _

  def addKey(obj: S3ObjectSummary): Long = {
    val objSize = obj.getSize
    size += objSize
    keys = keys :+ obj.getKey
    objSize
  }

  override def getLength: Long = keys.length

  override def getLocations: Array[String] = Array.empty

  override def write(out: DataOutput): Unit = {
    out.writeBoolean(null != sessionToken)
    if (null != sessionToken)
      out.writeUTF(sessionToken)
    out.writeUTF(bucket)
    out.writeInt(keys.length)
    keys.foreach(out.writeUTF)
  }

  override def readFields(in: DataInput): Unit = {
    if(in.readBoolean)
      sessionToken = in.readUTF
    else
      sessionToken = null
    bucket = in.readUTF
    val keyCount = in.readInt
    keys = for (i <- 1 to keyCount) yield in.readUTF
  }
}
