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

package geotrellis.layers.cog

import java.net.URI
import java.util.ServiceLoader

import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.index._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.util._
import spray.json._

import scala.reflect._

trait COGValueReader[ID] {
  val attributeStore: AttributeStore

  implicit def getByteReader(uri: URI): ByteReader
  implicit def getLayerId(id: ID): LayerId

  def reader[
    K: JsonFormat : SpatialComponent : ClassTag,
    V <: CellGrid[Int] : GeoTiffReader
  ](layerId: LayerId): COGReader[K, V]

  /** Produce a key value reader for a specific layer, prefetching layer metadata once at construction time */
  def baseReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader
  ](
    layerId: ID,
    keyPath: (K, Int, KeyIndex[K], ZoomRange) => String, // Key, maxWidth, toIndex, zoomRange
    fullPath: String => URI,
    exceptionHandler: K => PartialFunction[Throwable, Nothing] = { key: K => { case e: Throwable => throw e }: PartialFunction[Throwable, Nothing] }
   ): COGReader[K, V] = new COGReader[K, V] {
    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      attributeStore.readMetadata[COGLayerStorageMetadata[K]](LayerId(layerId.name, 0))

    def read(key: K): V = {
      val (zoomRange, spatialKey, overviewIndex, gridBounds) =
        cogLayerMetadata.getReadDefinition(key.getComponent[SpatialKey], layerId.zoom)

      val baseKeyIndex = keyIndexes(zoomRange)

      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val uri = fullPath(keyPath(key.setComponent(spatialKey), maxWidth, baseKeyIndex, zoomRange))

      try {
        GeoTiffReader[V].read(uri, streaming = true)
          .getOverview(overviewIndex)
          .crop(gridBounds)
          .tile
      } catch {
        case th: Throwable => exceptionHandler(key)(th)
      }

    }

    def readSubsetBands(key: K, bands: Seq[Int]): Array[Option[Tile]] = {
      val (zoomRange, spatialKey, overviewIndex, gridBounds) =
        cogLayerMetadata.getReadDefinition(key.getComponent[SpatialKey], layerId.zoom)

      val baseKeyIndex = keyIndexes(zoomRange)

      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val uri = fullPath(keyPath(key.setComponent(spatialKey), maxWidth, baseKeyIndex, zoomRange))

      val sourceGeoTiff = GeoTiffReader.readMultiband(uri, streaming = true)
      val sourceTile = sourceGeoTiff.getOverview(overviewIndex).tile

      // We first must determine which bands are valid and which are not
      // before doing the crop in order to avoid band subsetting errors
      // and/or loading unneeded data.
      val targetBandsWithIndex: Array[(Int, Int)] =
        bands
          .zipWithIndex
          .filter { case (band, _) =>
            band >= 0 && band < sourceGeoTiff.bandCount
          }
          .toArray

      val (targetBands, targetBandsIndexes) = targetBandsWithIndex.unzip

      val croppedTiles: Array[Tile] =
        try {
          sourceTile.cropBands(gridBounds, targetBands).bands.toArray
        } catch {
          case th: Throwable => exceptionHandler(key)(th)
        }

      val croppedTilesWithBandIndexes: Array[(Int, Tile)] = targetBandsIndexes.zip(croppedTiles)

      val returnedBands = Array.fill[Option[Tile]](bands.size)(None)

      for ((index, band) <- croppedTilesWithBandIndexes) {
        returnedBands(index) = Some(band)
      }

      returnedBands
    }
  }

  def overzoomingReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ? => TileResampleMethods[V]
  ](layerId: ID, resampleMethod: ResampleMethod = ResampleMethod.DEFAULT): COGReader[K, V]
}

object COGValueReader {

  /**
   * Produce COGValueReader instance based on URI description.
   * Find instances of [[COGValueReaderProvider]] through Java SPI.
   */
  def apply(attributeStore: AttributeStore, valueReaderUri: URI): COGValueReader[LayerId] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[COGValueReaderProvider])
      .iterator().asScala
      .find(_.canProcess(valueReaderUri))
      .getOrElse(throw new RuntimeException(s"Unable to find COGValueReaderProvider for $valueReaderUri"))
      .valueReader(valueReaderUri, attributeStore)
  }

  /**
   * Produce COGValueReader instance based on URI description.
   * Find instances of [[COGValueReaderProvider]] through Java SPI.
   */
  def apply(attributeStoreUri: URI, valueReaderUri: URI): COGValueReader[LayerId] =
    apply(AttributeStore(attributeStoreUri), valueReaderUri)

  /**
   * Produce COGValueReader instance based on URI description.
   * Find instances of [[COGValueReaderProvider]] through Java SPI.
   * Required [[AttributeStoreProvider]] instance will be found from the same URI.
   */
  def apply(uri: URI): COGValueReader[LayerId] =
    apply(attributeStoreUri = uri, valueReaderUri = uri)

  def apply(attributeStore: AttributeStore, valueReaderUri: String): COGValueReader[LayerId] =
    apply(attributeStore, new URI(valueReaderUri))

  def apply(attributeStoreUri: String, valueReaderUri: String): COGValueReader[LayerId] =
    apply(AttributeStore(new URI(attributeStoreUri)), new URI(valueReaderUri))

  def apply(uri: String): COGValueReader[LayerId] = {
    val _uri = new URI(uri)
    apply(attributeStoreUri = _uri, valueReaderUri = _uri)
  }
}
