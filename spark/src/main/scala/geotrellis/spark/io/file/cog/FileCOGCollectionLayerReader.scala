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

package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.file.KeyPathGenerator
import geotrellis.spark.io.index._
import geotrellis.util._

import spray.json.JsonFormat
import java.net.URI
import java.io.File

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class FileCOGCollectionLayerReader(val attributeStore: AttributeStore, catalogPath: String)
  extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: COGCollectionReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], indexFilterOnly: Boolean) = {
    val collectionReader = implicitly[COGCollectionReader[V]]

    // if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      attributeStore.read[COGLayerStorageMetadata[K]](LayerId(id.name, 0), "cog_metadata")

    val metadata = cogLayerMetadata.tileLayerMetadata(id.zoom)

    val queryKeyBounds: Seq[KeyBounds[K]] = tileQuery(metadata)

    val readDefinitions: Seq[(ZoomRange, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])])] =
      queryKeyBounds.map { case KeyBounds(minKey, maxKey) =>
        cogLayerMetadata.getReadDefinitions(
          KeyBounds(minKey.getComponent[SpatialKey], maxKey.getComponent[SpatialKey]),
          id.zoom
        )
      }

    val zoomRange = readDefinitions.head._1
    val baseKeyIndex = keyIndexes(zoomRange)

    val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, s"${id.name}/${zoomRange.slug}", maxWidth)
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

    val seq = collectionReader.read[K](
      keyPath            = keyPath,
      pathExists         = { new File(_).isFile },
      baseQueryKeyBounds = baseQueryKeyBounds,
      decomposeBounds    = decompose,
      readDefinitions    = readDefinitions.flatMap(_._2).groupBy(_._1)
    )

    new ContextCollection(seq, metadata)
  }
}


