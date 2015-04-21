package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._
import org.apache.spark._
import java.io.PrintWriter
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.model.ObjectMetadata
import scala.io.Source
import java.io.ByteArrayInputStream

/**
 * Stores and retrieves layer attributes in an S3 bucket in JSON format
 * 
 * @param bucket    S3 bucket to use for attribute store
 * @param layerKey  path in the bucket for given LayerId, not ending in "/"
 */
class S3AttributeStore(s3clientProvider: () => S3Client, bucket: String, layerKey: LayerId => String)
                      (implicit sc: SparkContext) extends AttributeStore {
  type ReadableWritable[T] = RootJsonFormat[T]

  /** NOTE:
   * S3 is eventually consistent, therefore it is possible to write an attribute and fail to read it
   * immediatly afterwards. It is not clear if this is a practical concern.
   * It could be remedied by some kind of time-out cache for both read/write in this class.
   */

  val s3Client = s3clientProvider()

  def attributePath(id: LayerId, attributeName: String): String =
    s"${layerKey(id)}/${attributeName}.json"
  
  def read[T: RootJsonFormat](layerId: LayerId, attributeName: String): T = {
    val key = attributePath(layerId, attributeName)

    val is = s3Client.getObject(bucket, key).getObjectContent()
    val json = Source.fromInputStream(is).mkString;
    is.close();
    json.parseJson.convertTo[T]
  }

  def write[T: RootJsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val key = attributePath(layerId, attributeName)
    val s = value.toJson.compactPrint
    val is = new ByteArrayInputStream(value.toJson.compactPrint.getBytes("UTF-8"))
    s3Client.putObject(bucket, key, is, new ObjectMetadata())
    //AmazonServiceException possible
  }
}
