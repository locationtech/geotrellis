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

package geotrellis.spark.resample

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector.Extent
import org.apache.spark.rdd._

abstract class LayerRDDZoomResampleMethods[
  K: SpatialComponent,
  V <: CellGrid[Int]: (? => TileResampleMethods[V])
](val self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends MethodExtensions[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    resampleToZoom(sourceZoom, targetZoom, None, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    method: ResampleMethod
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    resampleToZoom(sourceZoom, targetZoom, None, method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: TileBounds
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: TileBounds,
    method: ResampleMethod
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    resampleToZoom(sourceZoom, targetZoom, targetExtent, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent,
    method: ResampleMethod
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    val layout = ZoomedLayoutScheme.layoutForZoom(targetZoom, self.metadata.layout.extent, self.metadata.layout.tileLayout.tileCols)
    val targetGridBounds = layout.mapTransform(targetExtent)
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)
  }

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int ,
    targetGridBounds: Option[GridBounds[Int]] = None,
    method: ResampleMethod = NearestNeighbor
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    ZoomResample(self, sourceZoom, targetZoom, targetGridBounds, method)
}
