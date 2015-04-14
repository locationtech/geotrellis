package geotrellis.spark.io.s3

import java.io.{DataOutput, DataInput}
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce.InputSplit
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials, AnonymousAWSCredentials, BasicSessionCredentials}
import com.typesafe.scalalogging.slf4j._

/**
 * Represents are batch of keys to be read from an S3 bucket.
 * AWS credentials have already been discovered and provided by the S3InputFormat.
 */
class S3InputSplit extends InputSplit with Writable with LazyLogging
{
  var accessKeyId: String = null
  var secretKey: String = null
  var sessionToken: String = null
  var bucket: String = _
  var keys: Seq[String] = Seq.empty

  def credentials: AWSCredentials = {
    logger.debug(s"AWS Credentials: $accessKeyId:$secretKey")
    if (accessKeyId != null && secretKey != null && sessionToken != null)
      new BasicSessionCredentials(accessKeyId, secretKey, sessionToken)
    else if (accessKeyId != null && secretKey != null)
      new BasicAWSCredentials(accessKeyId, secretKey)    
    else
      new AnonymousAWSCredentials()
  }

  def setCredentials(cred: AWSCredentials) = cred match {
    case c: AnonymousAWSCredentials =>
      accessKeyId = null
      secretKey = null
      sessionToken = null
    case c: BasicAWSCredentials =>
      accessKeyId = c.getAWSAccessKeyId
      secretKey = c.getAWSSecretKey
      secretKey = null
    case c: BasicSessionCredentials =>
      accessKeyId = c.getAWSAccessKeyId
      secretKey = c.getAWSSecretKey
      secretKey = c.getSessionToken
    case _ =>
      throw new IllegalArgumentException("Can not handle $c")
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
    out.writeBoolean(null == sessionToken)
    if (null != sessionToken)
      out.writeUTF(sessionToken)
    out.writeUTF(bucket)
    out.writeInt(keys.length)
    keys.foreach(out.writeUTF)
  }
  
  override def readFields(in: DataInput): Unit = {    
    if (in.readBoolean){
      accessKeyId = in.readUTF
      secretKey = in.readUTF
    }else{
      accessKeyId = null
      secretKey = null
    }
    if(in.readBoolean)
      sessionToken = in.readUTF
    else
      sessionToken = null
    bucket = in.readUTF
    val keyCount = in.readInt
    keys = for (i <- 1 to keyCount) yield in.readUTF
  }
}