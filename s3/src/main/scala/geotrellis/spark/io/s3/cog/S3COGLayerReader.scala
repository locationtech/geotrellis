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

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster.{CellGrid, GridBounds, RasterExtent, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutLevel, ZoomedLayoutScheme}
import geotrellis.util._
import geotrellis.vector.Extent
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3COGLayerReader(val attributeStore: AttributeStore)(implicit sc: SparkContext)
  extends FilteringCOGLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: /*S3COGRDDReader: */ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
    //M: JsonFormat: GetComponent[?, Bounds[K]]: GetComponent[?, LayoutDefinition]: GetComponent[?, Extent]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    implicit val spatialComponent: SpatialComponent[K] = implicitly[SpatialComponent[SpatialKey]].asInstanceOf[SpatialComponent[K]]

    val rddReader = implicitly[S3COGRDDReader[Tile]].asInstanceOf[S3COGRDDReader[V]] // drity for now
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, _, _) = try {
      attributeStore.readLayerAttributes[S3COGLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val LayerAttributes(_, baseMetadata, baseKeyIndex, _) = try {
      attributeStore.readLayerAttributes[S3COGLayerHeader, M, K](id.copy(zoom = header.zoomRanges._1))
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    println(s"header: ${header}")

    val queryKeyBounds: Seq[KeyBounds[K]] = tileQuery(metadata)
    val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, Index.encode(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => baseKeyIndex.indexRanges(bounds)

    println(s"metadata: $metadata")
    println(s"metadata.extent: ${metadata}")
    println(s"baseMetadata: $baseMetadata")

    val layoutScheme = ZoomedLayoutScheme(LatLng, 512/*256*/) // keep in header? DONT FORGET THAT IT SHOULD CORRESPOND TO THE TIFF SCHEME

    val LayoutLevel(_, baseLayout) = layoutScheme.levelForZoom(header.zoomRanges._1)
    val LayoutLevel(_, layout) = layoutScheme.levelForZoom(id.zoom)

    //val sourceBounds = metadata.getComponent[Bounds[K]]
    val baseKeyBounds = baseMetadata.getComponent[Bounds[K]]

    def transformKey(key: K): K = {
      val extent = layout.extent
      val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
      val targetRe = RasterExtent(extent, baseLayout.layoutCols, baseLayout.layoutRows)
      val SpatialKey(sourceCol, sourceRow) = key.asInstanceOf[SpatialKey] //key.getComponent[SpatialKey]
      val (col, row) = {
        val (x, y) = sourceRe.gridToMap(sourceCol, sourceRow)
        targetRe.mapToGrid(x, y)
      }

      SpatialKey(col, row).asInstanceOf[K] //key.setComponent(SpatialKey(col, row))
    }

    def transformKeyToLayout(key: K, segmentLayout: LayoutDefinition): K = {
      val extent = layout.extent
      val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
      val targetRe = RasterExtent(extent, segmentLayout.layoutCols, segmentLayout.layoutRows)
      val SpatialKey(sourceCol, sourceRow) = key.asInstanceOf[SpatialKey] //key.getComponent[SpatialKey]

      val (col, row) = {
        val (x, y) = sourceRe.gridToMap(sourceCol, sourceRow)
        targetRe.mapToGrid(x, y)
      }

      SpatialKey(col, row).asInstanceOf[K] //key.setComponent(SpatialKey(col, row))
    }

    // (KeyBounds ->
    val baseQueryKeyBoundsMap: Seq[(KeyBounds[K], Seq[(KeyBounds[K], KeyBounds[K])])] = {
      queryKeyBounds
        .flatMap { qkb =>
          val KeyBounds(minKey, maxKey) = qkb

          KeyBounds(transformKey(minKey), transformKey(maxKey)).intersect(baseKeyBounds) match {
            case EmptyBounds => None
            case kb: KeyBounds[K] => Some(kb -> qkb)
          }
        }
        .groupBy(_._1)
        .toSeq
    }

    val baseQueryKeyBounds: Seq[KeyBounds[K]] = {
      queryKeyBounds
        .flatMap { qkb =>
          val KeyBounds(minKey, maxKey) = qkb

          KeyBounds(transformKey(minKey), transformKey(maxKey)).intersect(baseKeyBounds) match {
            case EmptyBounds => None
            case kb: KeyBounds[K] => Some(kb)
          }
        }
        .distinct
    }

    val overviewIndex = {
      val (_, e) = header.zoomRanges
      e - id.zoom - 1
    }

    val kbToGb: KeyBounds[K] => GridBounds = { kb =>
      val KeyBounds(minKey, maxKey) = kb
      val extent = layout.extent
      val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)

      val SpatialKey(minSourceCol, minSourceRow) = minKey.asInstanceOf[SpatialKey] //key.getComponent[SpatialKey]
      val (minCol, minRow) = sourceRe.gridToMap(minSourceCol, minSourceRow)

      val SpatialKey(maxSourceCol, maxSourceRow) = maxKey.asInstanceOf[SpatialKey] //key.getComponent[SpatialKey]
      val (maxCol, maxRow) = sourceRe.gridToMap(maxSourceCol, maxSourceRow)

      //GridBounds(minCol, minRow, maxCol, maxRow)
      null
    }

    val gbToKey: GridBounds => K = { gb =>
      //key.setComponent[SpatialKey](layout.mapTransform(layout.mapTransform(gb).center))
      layout.mapTransform(layout.mapTransform(gb).center).asInstanceOf[K]
    }

    val gbToGb: GridBounds => GridBounds = { gb =>
      baseLayout.mapTransform(layout.mapTransform(gb))
    }

    //baseLayout

    println(s"baseQueryKeyBounds: ${baseQueryKeyBounds}")
    println(s"queryKeyBounds: ${queryKeyBounds}")

    val rdd = rddReader.read[K](
      bucket = bucket,
      transformKey = null,
      keyPath = keyPath,
      indexToKey = null,
      keyToExtent = null,
      keyBoundsToExtent = null,
      queryKeyBounds = baseQueryKeyBounds,
      realQueryKeyBounds = queryKeyBounds,
      baseQueryKeyBounds = baseQueryKeyBoundsMap,
      decomposeBounds = decompose,
      gbToKey = gbToKey,
      gbToGb = gbToGb,
      sourceLayout = layout,
      filterIndexOnly = filterIndexOnly,
      overviewIndex = overviewIndex,
      cellSize = Some(layout.cellSize),
      Some(numPartitions)
    )

    new ContextRDD(rdd, metadata)
  }
}

object S3LayerReader {
  def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): S3LayerReader =
    new S3LayerReader(attributeStore)

  def apply(bucket: String, prefix: String)(implicit sc: SparkContext): S3LayerReader =
    apply(new S3AttributeStore(bucket, prefix))
}
