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

package geotrellis.store.s3.cog

import io.circe._

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.cog._
import geotrellis.store.index._
import geotrellis.store.s3._

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client

import scala.reflect.ClassTag
import java.net.URI

class S3COGValueReader(
  val attributeStore: AttributeStore,
  s3Client: => S3Client = S3ClientProducer.get()
) extends OverzoomingCOGValueReader {

  def reader[
    K: Decoder: SpatialComponent : ClassTag,
    V <: CellGrid[Int]: GeoTiffReader
  ](layerId: LayerId): COGReader[K, V] = {
    val header =
      try {
        attributeStore.readHeader[S3LayerHeader](LayerId(layerId.name, 0))
      } catch {
        case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId).initCause(e)
        case e: NoSuchBucketException => throw new LayerNotFoundError(layerId).initCause(e)
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
        case e: S3Exception if e.statusCode == 404 =>
          throw new ValueNotFoundError(key, layerId)
      }
    )
  }
}

object S3COGValueReader {
  def apply(attributeStore: S3AttributeStore): S3COGValueReader =
    new S3COGValueReader(attributeStore, attributeStore.client)
}
