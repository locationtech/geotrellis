/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import spray.json._
import DefaultJsonProtocol._
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata}
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion

import scala.util.matching.Regex
import scala.io.Source
import java.nio.charset.Charset
import java.io.ByteArrayInputStream

import geotrellis.layers.LayerId

/**
 * Stores and retrieves layer attributes in an S3 bucket in JSON format
 *
 * @param bucket    S3 bucket to use for attribute store
 * @param prefix    path in the bucket for given LayerId
 */
class S3AttributeStore(val bucket: String, val prefix: String) extends BlobLayerAttributeStore {

  def s3Client: S3Client = S3Client.DEFAULT
  import S3AttributeStore._

  /** NOTE:
   * S3 is eventually consistent, therefore it is possible to write an attribute and fail to read it
   * immediately afterwards. It is not clear if this is a practical concern.
   * It could be remedied by some kind of time-out cache for both read/write in this class.
   */

  def path(parts: String*) =
    parts
      .filter(_.nonEmpty)
      .map { s => if(s.endsWith("/")) s.dropRight(1) else s }
      .mkString("/")

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
      .exists(_.getKey.endsWith(s"${AttributeStore.Fields.metadata}${SEP}${layerId.name}${SEP}${layerId.zoom}.json"))

  def delete(layerId: LayerId, attributeName: String): Unit = {
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
    val keys = layerKeys(layerId).map(new KeyVersion(_)).toList
    s3Client.deleteObjects(bucket, keys)
    clearCache(layerId)
  }

  def layerIds: Seq[LayerId] =
    s3Client
      .listObjectsIterator(bucket, path(prefix, "_attributes/metadata"))
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

  override def availableZoomLevels(layerName: String): Seq[Int] = {
    s3Client
      .listObjectsIterator(bucket, path(prefix, s"_attributes/metadata${SEP}${layerName}"))
      .toList
      .flatMap { os =>
        val List(zoomStr, foundName) = new java.io.File(os.getKey).getName.split(SEP).reverse.take(2).toList
        if (foundName == layerName)
          Some(zoomStr.replace(".json", "").toInt)
        else
          None
      }
      .distinct
  }
}

object S3AttributeStore {
  final val SEP = "__"

  def apply(bucket: String, root: String) =
    new S3AttributeStore(bucket, root)

  def apply(bucket: String): S3AttributeStore =
    apply(bucket, "")
}
