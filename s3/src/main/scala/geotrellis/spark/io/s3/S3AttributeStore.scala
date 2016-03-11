package geotrellis.spark.io.s3

import java.nio.charset.Charset
import geotrellis.spark._
import geotrellis.spark.io._
import spray.json._
import DefaultJsonProtocol._
import com.amazonaws.services.s3.model.{ObjectMetadata, AmazonS3Exception}
import scala.io.Source
import java.io.ByteArrayInputStream

import scala.util.matching.Regex

/**
 * Stores and retrieves layer attributes in an S3 bucket in JSON format
 *
 * @param bucket    S3 bucket to use for attribute store
 * @param prefix  path in the bucket for given LayerId, not ending in "/"
 */
class S3AttributeStore(val bucket: String, val prefix: String) extends AttributeStore with BlobLayerAttributeStore {
  val s3Client: S3Client = S3Client.default
  import S3AttributeStore._

  /** NOTE:
   * S3 is eventually consistent, therefore it is possible to write an attribute and fail to read it
   * immediately afterwards. It is not clear if this is a practical concern.
   * It could be remedied by some kind of time-out cache for both read/write in this class.
   */

  def path(parts: String*) = parts.filter(_.nonEmpty).mkString("/")

  def attributePath(id: LayerId, attributeName: String): String =
    path(prefix, "_attributes", s"${attributeName}${SEP}${id.name}${SEP}${id.zoom}.json")

  def attributePrefix(attributeName: String): String =
    path(prefix, "_attributes", s"${attributeName}${SEP}")

  private def readKey[T: JsonFormat](key: String): (LayerId, T) = {
    val is = s3Client.getObject(bucket, key).getObjectContent
    val json =
      try {
        Source.fromInputStream(is)(Charset.forName("UTF-8")).mkString
      } finally {
        is.close()
      }

    json.parseJson.convertTo[(LayerId, T)]
  }

  def read[T: JsonFormat](layerId: LayerId, attributeName: String): T =
    try {
      readKey[T](attributePath(layerId, attributeName))._2
    } catch {
      case e: AmazonS3Exception =>
        throw new AttributeNotFoundError(attributeName, layerId).initCause(e)
    }

  def readAll[T: JsonFormat](attributeName: String): Map[LayerId, T] =
    s3Client
      .listObjectsIterator(bucket, attributePrefix(attributeName))
      .map{ os =>
        try {
          readKey[T](os.getKey)
        } catch {
          case e: AmazonS3Exception =>
            throw new LayerIOError(s"Unable to list $attributeName attributes from $bucket/${os.getKey}").initCause(e)
        }
      }
      .toMap

  def write[T: JsonFormat](layerId: LayerId, attributeName: String, value: T): Unit = {
    val key = attributePath(layerId, attributeName)
    val str = (layerId, value).toJson.compactPrint
    val is = new ByteArrayInputStream(str.getBytes("UTF-8"))
    s3Client.putObject(bucket, key, is, new ObjectMetadata())
    //AmazonServiceException possible
  }

  def layerExists(layerId: LayerId): Boolean =
    s3Client
      .listObjectsIterator(bucket, path(prefix, "_attributes"))
      .exists(_.getKey.endsWith(s"${SEP}${layerId.name}${SEP}${layerId.zoom}.json"))

  def delete(layerId: LayerId, attributeName: String): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)
    s3Client.deleteObject(bucket, attributePath(layerId, attributeName))
    clearCache(layerId, attributeName)
  }

  private def layerKeys(layerId: LayerId): Seq[String] = {
    s3Client
      .listObjectsIterator(bucket, path(prefix, "_attributes"))
      .map { _.getKey }
      .filter { _.contains(s"${SEP}${layerId.name}${SEP}${layerId.zoom}.json") }
      .toVector
  }

  def delete(layerId: LayerId): Unit = {
    if(!layerExists(layerId)) throw new LayerNotFoundError(layerId)
    layerKeys(layerId).foreach(s3Client.deleteObject(bucket, _))
    clearCache(layerId)
  }

  def layerIds: Seq[LayerId] =
    s3Client
      .listObjectsIterator(bucket, path(prefix, "_attributes"))
      .toList
      .map { os =>
        val List(zoomStr, name) = new java.io.File(os.getKey).getName.split(SEP).reverse.take(2).toList
        LayerId(name, zoomStr.replace(".json", "").toInt)
      }
      .distinct

  def availableAttributes(layerId: LayerId): Seq[String] = {
    layerKeys(layerId).map { key =>
      new java.io.File(key).getName.split(SEP).head
    }
  }
}

object S3AttributeStore {
  final val SEP = "__"

  def apply(bucket: String, root: String) =
    new S3AttributeStore(bucket, root)

  def apply(bucket: String): S3AttributeStore =
    apply(bucket, "")
}
