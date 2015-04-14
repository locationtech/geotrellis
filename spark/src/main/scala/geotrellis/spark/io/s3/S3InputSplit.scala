package geotrellis.spark.io.s3

import java.io.{DataOutput, DataInput}
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce.InputSplit
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials, AnonymousAWSCredentials}
import com.typesafe.scalalogging.slf4j._

/**
 * Represents are batch of keys to be read from an S3 bucket.
 * AWS credentials have already been discovered and provided by the S3InputFormat.
 */
class S3InputSplit extends InputSplit with Writable with LazyLogging
{
  var accessKeyId: String = null
  var secretKey: String = null
  var bucket: String = _
  var keys: Seq[String] = Seq.empty

  def credentials: AWSCredentials = {
    logger.debug(s"AWS Credentials: $accessKeyId:$secretKey")
    if (accessKeyId != null && secretKey != null)
      new BasicAWSCredentials(accessKeyId, secretKey)    
    else
      new AnonymousAWSCredentials()
  }

  override def getLength: Long = keys.length

  override def getLocations: Array[String] = Array.empty

  override def write(out: DataOutput): Unit = {    
    val haveAuth = accessKeyId != null && secretKey != null
    out.writeBoolean(haveAuth)
    if (haveAuth){
      out.writeUTF(accessKeyId)
      out.writeUTF(secretKey)
    }
    out.writeUTF(bucket)
    out.writeInt(keys.length)
    keys.foreach(out.writeUTF)
  }
  
  override def readFields(in: DataInput): Unit = {    
    if (in.readBoolean){
      accessKeyId = in.readUTF
      secretKey = in.readUTF
    }
    bucket = in.readUTF
    val keyCount = in.readInt
    keys = for (i <- 1 to keyCount) yield in.readUTF
  }
}