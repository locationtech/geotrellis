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

import geotrellis.raster.{CellGrid, RasterExtent}
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.{Index, MergeQueue}
import geotrellis.util._

import spray.json._
import java.net.URI

import scala.reflect._

abstract class COGCollectionLayerReader[ID] { self =>
  val attributeStore: AttributeStore

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: ID, rasterQuery: LayerQuery[K, TileLayerMetadata[K]]): Seq[(K, V)] with Metadata[TileLayerMetadata[K]]

  def baseRead[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](
    id: ID,
    tileQuery: LayerQuery[K, TileLayerMetadata[K]],
    getKeyPath: (ZoomRange, Int) => BigInt => String,
    pathExists: String => Boolean, // check the path above exists
    fullPath: String => URI, // add an fs prefix
    defaultThreads: Int
  )(implicit getByteReader: URI => ByteReader, idToLayerId: ID => LayerId): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      try {
        attributeStore.read[COGLayerStorageMetadata[K]](LayerId(id.name, 0), "cog_metadata")
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      }

    val metadata = cogLayerMetadata.tileLayerMetadata(id.zoom)

    val queryKeyBounds: Seq[KeyBounds[K]] = tileQuery(metadata)

    val readDefinitions: Seq[(ZoomRange, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])])] =
      cogLayerMetadata.getReadDefinitions(queryKeyBounds, id.zoom)

    readDefinitions.headOption.map(_._1) match {
      case Some(zoomRange) => {
        val baseKeyIndex = keyIndexes(zoomRange)

        val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
        val keyPath: BigInt => String = getKeyPath(zoomRange, maxWidth)
        val decompose = (bounds: KeyBounds[K]) => baseKeyIndex.indexRanges(bounds)

        val baseLayout = cogLayerMetadata.layoutForZoom(zoomRange.minZoom)
        val layout = cogLayerMetadata.layoutForZoom(id.zoom)

        val baseKeyBounds = cogLayerMetadata.zoomRangeInfoFor(zoomRange.minZoom)._2

        def transformKeyBounds(keyBounds: KeyBounds[K]): KeyBounds[K] = {
          val KeyBounds(minKey, maxKey) = keyBounds
          val extent = layout.extent
          val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
          val targetRe = RasterExtent(extent, baseLayout.layoutCols, baseLayout.layoutRows)

          val minSpatialKey = minKey.getComponent[SpatialKey]
          val (minCol, minRow) = {
            val (x, y) = sourceRe.gridToMap(minSpatialKey.col, minSpatialKey.row)
            targetRe.mapToGrid(x, y)
          }

          val maxSpatialKey = maxKey.getComponent[SpatialKey]
          val (maxCol, maxRow) = {
            val (x, y) = sourceRe.gridToMap(maxSpatialKey.col, maxSpatialKey.row)
            targetRe.mapToGrid(x, y)
          }

          KeyBounds(
            minKey.setComponent(SpatialKey(minCol, minRow)),
            maxKey.setComponent(SpatialKey(maxCol, maxRow))
          )
        }

        val baseQueryKeyBounds: Seq[KeyBounds[K]] =
          queryKeyBounds
            .flatMap { qkb =>
              transformKeyBounds(qkb).intersect(baseKeyBounds) match {
                case EmptyBounds => None
                case kb: KeyBounds[K] => Some(kb)
              }
            }
            .distinct

        val seq =
          COGCollectionLayerReader
            .read[K, V](
              keyPath = keyPath,
              pathExists = pathExists,
              fullPath = fullPath,
              baseQueryKeyBounds = baseQueryKeyBounds,
              decomposeBounds = decompose,
              threads = defaultThreads,
              readDefinitions = readDefinitions.flatMap(_._2).groupBy(_._1)
          )

        new ContextCollection(seq, metadata)
      }
      case _ =>
        ContextCollection(Seq(), metadata.setComponent[Bounds[K]](EmptyBounds))
    }
  }

  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ]: Reader[ID, Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Reader[ID, Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def read(id: ID): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] =
        self.read[K, V](id)
    }

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: ID): Seq[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read[K, V](id, new LayerQuery[K, TileLayerMetadata[K]])

  def query[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](layerId: ID): BoundLayerQuery[K, TileLayerMetadata[K], Seq[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, read[K, V](layerId, _))
}

object COGCollectionLayerReader {
  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader
  ](
     keyPath: BigInt => String, // keyPath
     pathExists: String => Boolean, // check the path above exists
     fullPath: String => URI, // add an fs prefix
     baseQueryKeyBounds: Seq[KeyBounds[K]], // each key here represents a COG filename
     decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
     readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
     threads: Int,
     numPartitions: Option[Int] = None
   )(implicit getByteReader: URI => ByteReader): Seq[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    LayerReader.njoin[K, V](ranges.toIterator, threads) { index: BigInt =>
      if (!pathExists(keyPath(index))) Vector()
      else {
        val uri = fullPath(keyPath(index))
        val byteReader: ByteReader = uri
        val baseKey =
          TiffTagsReader
            .read(byteReader)
            .tags
            .headTags(GTKey)
            .parseJson
            .convertTo[K]

        readDefinitions
          .get(baseKey.getComponent[SpatialKey])
          .toVector
          .flatten
          .flatMap { case (spatialKey, overviewIndex, _, seq) =>
            val key = baseKey.setComponent(spatialKey)
            val tiff = GeoTiffReader[V].read(uri, decompress = false, streaming = true).getOverview(overviewIndex)
            val map = seq.map { case (gb, sk) => gb -> key.setComponent(sk) }.toMap

            tiff
              .crop(map.keys.toSeq)
              .flatMap { case (k, v) => map.get(k).map(i => i -> v) }
              .toVector 
          }
      }
    }
  }
}
