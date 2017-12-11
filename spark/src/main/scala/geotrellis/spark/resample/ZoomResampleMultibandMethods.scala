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

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent

abstract class ZoomResampleMultibandMethods[K: SpatialComponent](val self: MultibandTileLayerRDD[K]) extends MethodExtensions[MultibandTileLayerRDD[K]] {
  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int
  ): MultibandTileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, None, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    method: ResampleMethod
  ): MultibandTileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, None, method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: GridBounds
  ): MultibandTileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: GridBounds,
    method: ResampleMethod
  ): MultibandTileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent
  ): MultibandTileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, targetExtent, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent,
    method: ResampleMethod
  ): MultibandTileLayerRDD[K] = {
    val layout = ZoomedLayoutScheme.layoutForZoom(targetZoom, self.metadata.layout.extent, self.metadata.layout.tileLayout.tileCols)
    val targetGridBounds = layout.mapTransform(targetExtent)
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)
  }

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int ,
    targetGridBounds: Option[GridBounds] = None,
    method: ResampleMethod = NearestNeighbor
  ): MultibandTileLayerRDD[K] =
    ZoomResampleMultiband(self, sourceZoom, targetZoom, targetGridBounds, method)
}
