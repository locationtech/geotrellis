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

package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.util._

import spray.json._
import java.net.URI

import scala.reflect._

trait COGValueReader[ID] {
  val attributeStore: AttributeStore

  implicit def getByteReader(uri: URI): ByteReader
  implicit def idLayerId(id: ID): LayerId

  def reader[
    K: JsonFormat : SpatialComponent : ClassTag,
    V <: CellGrid : TiffMethods
  ](layerId: LayerId): Reader[K, V]

  /** Produce a key value reader for a specific layer, prefetching layer metadata once at construction time */
  def baseReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: TiffMethods
  ](
    layerId: ID,
    keyPath: (K, Int, KeyIndex[K], ZoomRange) => String, // Key, maxWidth, toIndex, zoomRange
    fullPath: String => URI,
    exceptionHandler: K => PartialFunction[Throwable, Nothing] = { key: K => { case e: Throwable => throw e }: PartialFunction[Throwable, Nothing] }
   ): Reader[K, V] = new Reader[K, V] {
    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      attributeStore.read[COGLayerStorageMetadata[K]](LayerId(layerId.name, 0), "cog_metadata")

    val tiffMethods: TiffMethods[V] = implicitly[TiffMethods[V]]

    def read(key: K): V = {
      val (zoomRange, spatialKey, overviewIndex, gridBounds) =
        cogLayerMetadata.getReadDefinition(key.getComponent[SpatialKey], layerId.zoom)

      val baseKeyIndex = keyIndexes(zoomRange)

      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val uri = fullPath(keyPath(key.setComponent(spatialKey), maxWidth, baseKeyIndex, zoomRange))

      try {
        val tiff = tiffMethods.readTiff(uri, overviewIndex)
        tiff.crop(gridBounds).tile
      } catch {
        case th: Throwable => exceptionHandler(key)(th)
      }
    }
  }

  def overzoomingReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: TiffMethods: ? => TileResampleMethods[V]
  ](layerId: ID, resampleMethod: ResampleMethod = ResampleMethod.DEFAULT): Reader[K, V]
}


