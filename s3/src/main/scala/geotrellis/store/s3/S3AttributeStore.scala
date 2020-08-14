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

package geotrellis.store.s3

import geotrellis.store._

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._
import org.apache.commons.io.IOUtils
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.parser._
import cats.syntax.either._

import scala.collection.JavaConverters._
import java.nio.charset.Charset

/**
 * Stores and retrieves layer attributes in an S3 bucket in JSON format
 *
 * @param bucket    S3 bucket to use for attribute store
 * @param prefix    path in the bucket for given LayerId
 */
class S3AttributeStore(
  val bucket: String,
  val prefix: String,
  s3Client: => S3Client = S3ClientProducer.get()
) extends BlobLayerAttributeStore {
  import S3AttributeStore._

  @transient lazy val client = s3Client

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

  private def readKey[T: Decoder](key: String): (LayerId, T) = {
    val getRequest = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    val s3objStream = s3Client.getObject(getRequest)
    val json =
      try {
        IOUtils.toString(s3objStream, Charset.forName("UTF-8"))
      } finally {
        s3objStream.close()
      }

    parse(json).flatMap(_.as[(LayerId, T)]).valueOr(throw _)
  }

  def read[T: Decoder](layerId: LayerId, attributeName: String): T =
    try {
      readKey[T](attributePath(layerId, attributeName))._2
    } catch {
      case e: S3Exception =>
        throw new AttributeNotFoundError(attributeName, layerId).initCause(e)
    }

  def readAll[T: Decoder](attributeName: String): Map[LayerId, T] = {
    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(attributePrefix(attributeName))
      .build()
    s3Client
      .listObjectsV2Paginator(listRequest)
      .contents
      .asScala
      .map{ s3obj =>
        try {
          readKey[T](s3obj.key)
        } catch {
          case e: S3Exception =>
            throw new LayerIOError(s"Unable to list $attributeName attributes from $bucket/${s3obj.key}").initCause(e)
        }
      }
      .toMap
  }

  def write[T: Encoder](layerId: LayerId, attributeName: String, value: T): Unit = {
    val key = attributePath(layerId, attributeName)
    val str = (layerId, value).asJson.noSpaces
    val putRequest = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    val requestBody = RequestBody.fromBytes(str.getBytes("UTF-8"))
    s3Client.putObject(putRequest, requestBody)
    //AmazonServiceException possible
  }

  def layerExists(layerId: LayerId): Boolean = {
    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(path(prefix, "_attributes"))
      .build()
    s3Client
      .listObjectsV2Paginator(listRequest)
      .contents
      .asScala
      .exists(_.key.endsWith(s"${AttributeStore.Fields.metadata}${SEP}${layerId.name}${SEP}${layerId.zoom}.json"))
  }

  def delete(layerId: LayerId, attributeName: String): Unit = {
    val deleteRequest = DeleteObjectRequest.builder()
      .bucket(bucket)
      .key(attributePath(layerId, attributeName))
      .build()
    s3Client.deleteObject(deleteRequest)
    clearCache(layerId, attributeName)
  }

  private def layerKeys(layerId: LayerId): Seq[String] = {
    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(path(prefix, "_attributes"))
      .build()
    s3Client
      .listObjectsV2Paginator(listRequest)
      .contents
      .asScala
      .map { _.key }
      .filter { _.contains(s"${SEP}${layerId.name}${SEP}${layerId.zoom}.json") }
      .toSeq
  }

  def delete(layerId: LayerId): Unit = {
    val identifiers =
      layerKeys(layerId).map { key => ObjectIdentifier.builder().key(key).build() }
    val deleteDefinition = Delete.builder()
      .objects(identifiers:_*)
      .build()
    val deleteRequest = DeleteObjectsRequest.builder()
      .bucket(bucket)
      .delete(deleteDefinition)
      .build()
    s3Client.deleteObjects(deleteRequest)
    clearCache(layerId)
  }

  def layerIds: Seq[LayerId] = {
    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(path(prefix, "_attributes/metadata"))
      .build()
    s3Client
      .listObjectsV2Paginator(listRequest)
      .contents
      .asScala
      .map { s3obj =>
        val List(zoomStr, name) = new java.io.File(s3obj.key).getName.split(SEP).reverse.take(2).toList
        LayerId(name, zoomStr.replace(".json", "").toInt)
      }
      .toSeq
      .distinct
  }

  def availableAttributes(layerId: LayerId): Seq[String] = {
    layerKeys(layerId).map { key =>
      new java.io.File(key).getName.split(SEP).head
    }
  }

  override def availableZoomLevels(layerName: String): Seq[Int] = {
    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(path(prefix, s"_attributes/metadata${SEP}${layerName}"))
      .build()
    s3Client
      .listObjectsV2Paginator(listRequest)
      .contents
      .asScala
      .flatMap { s3obj =>
        val List(zoomStr, foundName) = new java.io.File(s3obj.key).getName.split(SEP).reverse.take(2).toList
        if (foundName == layerName)
          Some(zoomStr.replace(".json", "").toInt)
        else
          None
      }
      .toSeq
      .distinct
  }
}

object S3AttributeStore {
  final val SEP = "__"

  def apply(bucket: String, root: String, s3Client: => S3Client = S3ClientProducer.get()) =
    new S3AttributeStore(bucket, Option(root).getOrElse(""), s3Client)

  def apply(bucket: String, s3Client: => S3Client): S3AttributeStore =
    apply(bucket, "", s3Client)
}
