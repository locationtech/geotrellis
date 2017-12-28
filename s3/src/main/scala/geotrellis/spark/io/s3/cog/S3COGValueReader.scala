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

package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3.S3Client
import geotrellis.util._

import spray.json._
import com.amazonaws.services.s3.model.AmazonS3Exception

import scala.reflect.ClassTag

import java.net.URI

class S3COGValueReader(
  val attributeStore: AttributeStore,
  val bucket: String,
  val prefix: String
) extends OverzoomingCOGValueReader {

  def s3Client: S3Client = S3Client.DEFAULT

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, s3Client)

  def reader[
    K: JsonFormat: SpatialComponent : ClassTag,
    V <: CellGrid: TiffMethods
  ](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      attributeStore.read[COGLayerStorageMetadata[K]](LayerId(layerId.name, 0), "cog_metadata")

    val tiffMethods: TiffMethods[V] = implicitly[TiffMethods[V]]

    def read(key: K): V = {
      val (zoomRange, spatialKey, overviewIndex, gridBounds) =
        cogLayerMetadata.getReadDefinition(key.getComponent[SpatialKey], layerId.zoom)

      val baseKeyIndex = keyIndexes(zoomRange)

      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val path = (k: K) => s"$bucket/$prefix/${Index.encode(baseKeyIndex.toIndex(k), maxWidth)}.${Extension}"

      val uri = new URI(s"s3://${path(key.setComponent(spatialKey))}")

      try {
        val tiff = tiffMethods.readTiff(uri, overviewIndex)
        tiffMethods.cropTiff(tiff, gridBounds)
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 =>
          throw new ValueNotFoundError(key, layerId)
      }
    }
  }
}

