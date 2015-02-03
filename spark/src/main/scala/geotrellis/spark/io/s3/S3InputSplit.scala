package geotrellis.spark.io.s3

import java.io.{DataOutput, DataInput}
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce.InputSplit
import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials, AnonymousAWSCredentials}

/**
 * Represents are batch of keys to be read from an S3 bucket.
 * AWS credentials have already been discovered and provided by the S3InputFormat.
 */
class S3InputSplit extends InputSplit with Writable 
{
  var haveAuth = false
  var accessKeyId: String = _
  var secretKey: String = _
  var bucket: String = _
  var keys: Seq[String] = Seq.empty

  def credentials: AWSCredentials =
    if (haveAuth)
      new BasicAWSCredentials(accessKeyId, secretKey)    
    else
      new AnonymousAWSCredentials()

  override def getLength: Long = keys.length

  override def getLocations: Array[String] = Array.empty

  override def write(out: DataOutput): Unit = {
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
    haveAuth = in.readBoolean
    if (accessKeyId != null && secretKey != null){
      accessKeyId = in.readUTF
      secretKey = in.readUTF
    }
    bucket = in.readUTF
    val keyCount = in.readInt
    keys = for (i <- 1 to keyCount) yield in.readUTF
  }
}