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

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.util._
import geotrellis.store.cog._
import geotrellis.store.index._
import geotrellis.store.s3._

import cats.effect._
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import java.net.URI

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class S3COGCollectionLayerReader(
  val attributeStore: AttributeStore,
  s3Client: => S3Client = S3ClientProducer.get(),
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends COGCollectionLayerReader[LayerId] {

  @transient implicit lazy val ioRuntime: unsafe.IORuntime = runtime

  def read[
    K: SpatialComponent: Boundable: Decoder: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, rasterQuery: LayerQuery[K, TileLayerMetadata[K]]) = {
    val header =
      try {
        attributeStore.readHeader[S3LayerHeader](LayerId(id.name, 0))
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
        case e: NoSuchBucketException => throw new LayerNotFoundError(id).initCause(e)
      }

    val bucket = header.bucket
    val prefix = header.key

    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"$bucket/$prefix/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.${Extension}"

    baseRead[K, V](
      id              = id,
      tileQuery       = rasterQuery,
      getKeyPath      = getKeyPath,
      pathExists      = { s3Client.objectExists(_) },
      fullPath        = { path => new URI(s"s3://$path") }
    )
  }
}

object S3COGCollectionLayerReader {
  def apply(attributeStore: S3AttributeStore): S3COGCollectionLayerReader =
    new S3COGCollectionLayerReader(
      attributeStore,
      attributeStore.client
    )

  def apply(bucket: String, prefix: String, s3Client: => S3Client = S3ClientProducer.get()): S3COGCollectionLayerReader = {
    val attStore = S3AttributeStore(bucket, prefix, s3Client)
    apply(attStore)
  }
}
