package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._
import DefaultJsonProtocol._

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
class S3AttributeStore(s3Client: S3Client, bucket: String, rootPath: String)
                      (implicit sc: SparkContext) extends AttributeStore {
  type ReadableWritable[T] = RootJsonFormat[T]

  /** NOTE:
   * S3 is eventually consistent, therefore it is possible to write an attribute and fail to read it
   * immediatly afterwards. It is not clear if this is a practical concern.
   * It could be remedied by some kind of time-out cache for both read/write in this class.
   */

  def path(parts: String*) = parts.filter(_.nonEmpty).mkString("/")

  def attributePath(id: LayerId, attributeName: String): String =
    path(rootPath, "_attributes", s"${attributeName}__${id.name}__${id.zoom}.json")

  def attributePrefix(attributeName: String): String =
    path(rootPath, "_attributes", s"${attributeName}__")

  private def readKey[T: ReadableWritable](key: String): Option[(LayerId, T)] = {
    val is = s3Client.getObject(bucket, key).getObjectContent()
    val json = Source.fromInputStream(is).mkString
    is.close()
    Some(json.parseJson.convertTo[(LayerId, T)])
    // TODO: Make this crash to find out when None should be returned
  }
  
  def read[T: ReadableWritable](layerId: LayerId, attributeName: String): T =
    readKey[T](attributePath(layerId, attributeName)) match {
      case Some((id, value)) => value
      case None => throw new AttributeNotFoundError(attributeName, layerId)
    }

  def readAll[T: ReadableWritable](attributeName: String): Map[LayerId, T] =    
    s3Client
      .listObjectsIterator(bucket, attributePrefix(attributeName))
      .map{ os =>       
        readKey[T](os.getKey) match {
          case Some(tup) => tup
          case None => throw new CatalogError(s"Unable to list $attributeName attributes from $bucket/${os.getKey}") 
        }
      }
      .toMap

  def write[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit = {
    val key = attributePath(layerId, attributeName)
    val str = (layerId, value).toJson.compactPrint
    val is = new ByteArrayInputStream(str.getBytes("UTF-8"))
    s3Client.putObject(bucket, key, is, new ObjectMetadata())
    //AmazonServiceException possible
  }
}
