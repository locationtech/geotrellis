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
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.tiling.SpatialComponent
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.layers.io.index._
import geotrellis.spark.io.s3.{S3AttributeStore, S3Client, S3LayerHeader}
import geotrellis.util._
import spray.json._
import com.amazonaws.services.s3.model.AmazonS3Exception

import scala.reflect.ClassTag
import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.io.cog.{COGReader, OverzoomingCOGValueReader, ZoomRange}

class S3COGValueReader(
  val attributeStore: AttributeStore
) extends OverzoomingCOGValueReader {

  def s3Client: S3Client = S3Client.DEFAULT

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, s3Client)

  def reader[
    K: JsonFormat: SpatialComponent : ClassTag,
    V <: CellGrid[Int]: GeoTiffReader
  ](layerId: LayerId): COGReader[K, V] = {
    val header =
      try {
        attributeStore.readHeader[S3LayerHeader](LayerId(layerId.name, 0))
      } catch {
        case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId).initCause(e)
      }

    def keyPath(key: K, maxWidth: Int, baseKeyIndex: KeyIndex[K], zoomRange: ZoomRange): String =
      s"${header.bucket}/${header.key}/${layerId.name}/" +
      s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
      s"${Index.encode(baseKeyIndex.toIndex(key), maxWidth)}.${Extension}"

    baseReader[K, V](
      layerId,
      keyPath,
      path => new URI(s"s3://${path}"),
      key => {
        case e: AmazonS3Exception if e.getStatusCode == 404 =>
          throw new ValueNotFoundError(key, layerId)
      }
    )
  }
}

object S3COGValueReader {
  def apply(s3attributeStore: S3AttributeStore): S3COGValueReader =
    new S3COGValueReader(s3attributeStore) {
      override def s3Client: S3Client = s3attributeStore.s3Client
    }
}
