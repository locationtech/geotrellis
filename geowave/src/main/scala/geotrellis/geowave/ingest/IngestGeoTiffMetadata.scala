/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.ingest

import geotrellis.geowave.dsl.{Metadata, TilingBounds, VoxelBounds3D, VoxelDimensions3D}
import geotrellis.raster.geotiff.GeoTiffMetadata
import geotrellis.raster.split.Split
import geotrellis.vector.Extent

case class IngestGeoTiffMetadata(
  bounds: VoxelBounds3D,
  extent: Extent,
  metadata: GeoTiffMetadata
) extends Metadata {
  type VoxelBoundsInternal = VoxelBounds3D

  def split(tb: TilingBounds): Seq[IngestGeoTiffMetadata] = split(bounds.toVoxelDimensions.withTilingBounds(tb))
  def split(tb: TilingBounds, options: Split.Options): Seq[IngestGeoTiffMetadata] = split(bounds.toVoxelDimensions.withTilingBounds(tb), options)
  def split(dims: VoxelDimensions3D): Seq[IngestGeoTiffMetadata] = split(dims, Split.Options.DEFAULT)
  def split(dims: VoxelDimensions3D, options: Split.Options): Seq[IngestGeoTiffMetadata] = {
    val mt = mapKeyTransform
    bounds
      .split(dims, options)
      .map { bounds =>
        if(this.bounds.toGridBounds == bounds.toGridBounds) this.copy(bounds = bounds)
        else this.copy(bounds = bounds, extent = mt.boundsToExtent(bounds.toGridBounds))
      }
  }
}