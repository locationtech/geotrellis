/*
 * Copyright 2017 Azavea
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

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression.{Compression, NoCompression}
import geotrellis.layers.{LayerId, Metadata, TileLayerMetadata}
import geotrellis.layers._
import geotrellis.layers.cog.{COGLayerStorageMetadata, ZoomRange}
import geotrellis.layers.index._
import geotrellis.spark._

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.rdd.RDD

import spray.json._

import java.net.URI
import java.util.ServiceLoader

import scala.reflect._


trait COGLayerWriter extends LazyLogging with Serializable {
  import COGLayerWriter.Options

  val attributeStore: AttributeStore

  def writeCOGLayer[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]],
    mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]] = None
  ): Unit

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     keyIndexMethod: KeyIndexMethod[K]
   ): Unit = write[K, V](layerName, tiles, tileZoom, keyIndexMethod, None, Options.DEFAULT)

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     keyIndexMethod: KeyIndexMethod[K],
     options: Options
   ): Unit = write[K, V](layerName, tiles, tileZoom, keyIndexMethod, None, options)

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
    layerName: String,
    tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    tileZoom: Int,
    keyIndexMethod: KeyIndexMethod[K],
    mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]],
    options: Options
  ): Unit =
    tiles.metadata.bounds match {
      case keyBounds: KeyBounds[K] =>
        val cogLayer = COGLayer.fromLayerRDD(tiles, tileZoom, options = options)
        // println(cogLayer.metadata.toJson.prettyPrint)
        val keyIndexes: Map[ZoomRange, KeyIndex[K]] =
          cogLayer.metadata.zoomRangeInfos.
            map { case (zr, bounds) => zr -> keyIndexMethod.createIndex(bounds) }.
            toMap
        writeCOGLayer(layerName, cogLayer, keyIndexes, mergeFunc)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     keyIndex: KeyIndex[K]
   ): Unit = write[K, V](layerName, tiles, tileZoom, keyIndex, None, Options.DEFAULT)

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     keyIndex: KeyIndex[K],
     options: Options
   ): Unit = write[K, V](layerName, tiles, tileZoom, keyIndex, None, options)

  def write[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     keyIndex: KeyIndex[K],
     mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]],
     options: Options
   ): Unit =
    tiles.metadata.bounds match {
      case keyBounds: KeyBounds[K] =>
        val cogLayer = COGLayer.fromLayerRDD(tiles, tileZoom, options = options)
        val keyIndexes: Map[ZoomRange, KeyIndex[K]] =
          cogLayer.metadata.zoomRangeInfos.
            map { case (zr, _) => zr -> keyIndex }.
            toMap

        writeCOGLayer(layerName, cogLayer, keyIndexes, mergeFunc)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def writer[
    K: SpatialComponent: Boundable: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[LayerId, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Writer[LayerId, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def write(layerId: LayerId, layer: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
        COGLayerWriter.this.write[K, V](layerId.name, layer, layerId.zoom, keyIndexMethod)
    }

  def writer[
    K: SpatialComponent: Boundable: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](keyIndex: KeyIndex[K]):  Writer[LayerId, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Writer[LayerId, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def write(layerId: LayerId, layer: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
        COGLayerWriter.this.write[K, V](layerId.name, layer, layerId.zoom, keyIndex)
    }

  def overwrite[
    K: SpatialComponent: Boundable: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
    layerName: String,
    tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    tileZoom: Int,
    options: Options = Options.DEFAULT
  ): Unit =
    if(tiles.metadata.bounds.nonEmpty) update[K, V](layerName, tiles, tileZoom, None, options)
    else logger.info("Skipping layer update with empty bounds rdd.")

  def update[
    K: SpatialComponent: Boundable: Ordering: JsonFormat: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]: ? => TileCropMethods[V]: GeoTiffReader: GeoTiffBuilder
  ](
     layerName: String,
     tiles: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
     tileZoom: Int,
     mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]],
     options: Options = Options.DEFAULT
   ): Unit = {
    (tiles.metadata.bounds, mergeFunc) match {
      case (keyBounds: KeyBounds[K], _) =>
        val COGLayerStorageMetadata(metadata, keyIndexes) =
          try {
            attributeStore.readMetadata[COGLayerStorageMetadata[K]](LayerId(layerName, 0))
          } catch {
            // to follow GeoTrellis Layer Readers logic
            case e: AttributeNotFoundError => throw new LayerNotFoundError(LayerId(layerName, 0)).initCause(e)
          }

        val indexKeyBounds = keyIndexes(metadata.zoomRangeFor(tileZoom)).keyBounds

        // TODO: doublecheck this condition, it's more a workaround at the moment
        if(!indexKeyBounds.contains(keyBounds) && !metadata.keyBoundsForZoom(tileZoom).contains(keyBounds))
          throw new LayerOutOfKeyBoundsError(LayerId(layerName, tileZoom), indexKeyBounds)

        val cogLayer = COGLayer.fromLayerRDD(tiles, tileZoom, options = options)
        val ucogLayer = cogLayer.copy(metadata = cogLayer.metadata.combine(metadata))

        writeCOGLayer(layerName, ucogLayer, keyIndexes, mergeFunc)
      case (EmptyBounds, _) =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }
  }
}

object COGLayerWriter {

  case class Options(
    maxTileSize: Int = DefaultMaxTileSize,
    resampleMethod: ResampleMethod = NearestNeighbor,
    compression: Compression = NoCompression
  )

  object Options {
    def DEFAULT = Options()

    implicit def maxTileSizeToOptions(maxTileSize: Int): Options = Options(maxTileSize = maxTileSize)
    implicit def resampleMethodToOptions(resampleMethod: ResampleMethod): Options = Options(resampleMethod = resampleMethod)
    implicit def compressionToOption(compression: Compression): Options = Options(compression = compression)
  }

  private val DefaultMaxTileSize = 4096

  /**
   * Produce COGLayerWriter instance based on URI description.
   * Find instances of [[COGLayerWriterProvider]] through Java SPI.
   */
  def apply(attributeStore: AttributeStore, layerWriterUri: URI): COGLayerWriter = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[COGLayerWriterProvider])
      .iterator().asScala
      .find(_.canProcess(layerWriterUri))
      .getOrElse(throw new RuntimeException(s"Unable to find a COGLayerWriterProvider for $layerWriterUri"))
      .layerWriter(layerWriterUri, attributeStore)
  }

  /**
   * Produce COGLayerWriter instance based on URI description.
   * Find instances of [[COGLayerWriterProvider]] through Java SPI.
   */
  def apply(attributeStoreUri: URI, layerWriterUri: URI): COGLayerWriter =
    apply(attributeStore = AttributeStore(attributeStoreUri), layerWriterUri)

  /**
   * Produce COGLayerWriter instance based on URI description.
   * Find instances of [[COGLayerWriterProvider]] through Java SPI.
   * Required [[AttributeStoreProvider]] instance will be found from the same URI.
   */
  def apply(uri: URI): COGLayerWriter =
    apply(attributeStoreUri = uri, layerWriterUri = uri)

  def apply(attributeStore: AttributeStore, layerWriterUri: String): COGLayerWriter =
    apply(attributeStore, new URI(layerWriterUri))

  def apply(attributeStoreUri: String, layerWriterUri: String): COGLayerWriter =
    apply(new URI(attributeStoreUri), new URI(layerWriterUri))

  def apply(uri: String): COGLayerWriter =
    apply(new URI(uri))
}
